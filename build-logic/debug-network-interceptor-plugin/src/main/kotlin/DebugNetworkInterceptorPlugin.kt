import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import javax.inject.Inject

open class DebugNetworkInterceptorExtension @Inject constructor(objects: ObjectFactory) {
    val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val logEnabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val includeDependencies: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
}

class DebugNetworkInterceptorPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "debugNetworkInterceptor",
            DebugNetworkInterceptorExtension::class.java
        )

        project.pluginManager.withPlugin("com.android.application") {
            val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            project.logger.lifecycle("[DebugNetworkInterceptorPlugin] applied project=${project.path}")

            androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) { variant ->
                if (!extension.enabled.get()) {
                    project.logger.lifecycle(
                        "[DebugNetworkInterceptorPlugin] variant=${variant.name} enabled=false"
                    )
                    return@onVariants
                }

                val scope = if (extension.includeDependencies.get()) {
                    InstrumentationScope.ALL
                } else {
                    InstrumentationScope.PROJECT
                }

                project.logger.lifecycle(
                    "[DebugNetworkInterceptorPlugin] variant=${variant.name} enabled=true " +
                            "scope=$scope logEnabled=${extension.logEnabled.get()}"
                )

                variant.instrumentation.transformClassesWith(
                    DebugNetworkInterceptorClassVisitorFactory::class.java,
                    scope
                ) { parameters ->
                    parameters.variantName.set(variant.name)
                    parameters.logEnabled.set(extension.logEnabled)
                }
                variant.instrumentation.setAsmFramesComputationMode(
                    FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
                )
            }
        }
    }
}

interface DebugNetworkInterceptorParameters : InstrumentationParameters {
    @get:Input
    val variantName: Property<String>

    @get:Input
    val logEnabled: Property<Boolean>
}

abstract class DebugNetworkInterceptorClassVisitorFactory :
    AsmClassVisitorFactory<DebugNetworkInterceptorParameters> {

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return DebugNetworkInterceptorClassVisitor(
            nextClassVisitor,
            parameters.get().variantName.get(),
            parameters.get().logEnabled.get(),
            classContext.currentClassData.isApplicationSubclass()
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        val className = classData.className
        return !className.startsWith("com.debugtoolkit.networkinterceptor.")
                && !className.startsWith("com/debugtoolkit/networkinterceptor/")
                && !className.startsWith("okhttp3.")
                && !className.startsWith("okhttp3/")
    }
}

private class DebugNetworkInterceptorClassVisitor(
    nextClassVisitor: ClassVisitor,
    private val variantName: String,
    private val logEnabled: Boolean,
    private val isApplicationClass: Boolean
) : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {

    private var currentClassName: String = ""

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        currentClassName = name.orEmpty()
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val nextMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        return DebugNetworkInterceptorMethodVisitor(
            nextMethodVisitor,
            variantName,
            logEnabled,
            isApplicationClass,
            currentClassName,
            name.orEmpty(),
            descriptor.orEmpty()
        )
    }
}

private class DebugNetworkInterceptorMethodVisitor(
    nextMethodVisitor: MethodVisitor,
    private val variantName: String,
    private val logEnabled: Boolean,
    private val isApplicationClass: Boolean,
    private val className: String,
    private val methodName: String,
    private val methodDescriptor: String
) : MethodVisitor(Opcodes.ASM9, nextMethodVisitor) {

    override fun visitCode() {
        super.visitCode()
        if (!isApplicationClass || methodName != "onCreate" || methodDescriptor != "()V") {
            return
        }

        val continueLabel = Label()
        super.visitVarInsn(Opcodes.ALOAD, 0)
        super.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            DEBUG_NETWORK_PROCESS_GUARD_OWNER,
            "shouldSkipApplicationOnCreate",
            DEBUG_NETWORK_PROCESS_GUARD_DESCRIPTOR,
            false
        )
        super.visitJumpInsn(Opcodes.IFEQ, continueLabel)
        super.visitInsn(Opcodes.RETURN)
        super.visitLabel(continueLabel)

        if (logEnabled) {
                println(
                    "[DebugNetworkInterceptorPlugin] inject application guard variant=$variantName " +
                            "class=$className method=$methodName$methodDescriptor " +
                            "guard=$DEBUG_NETWORK_PROCESS_GUARD_OWNER.shouldSkipApplicationOnCreate"
                )
        }
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        if (
            opcode == Opcodes.INVOKEVIRTUAL &&
            owner == OKHTTP_BUILDER_OWNER &&
            name == "build" &&
            descriptor == OKHTTP_BUILD_DESCRIPTOR
        ) {
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                DEBUG_NETWORK_MANAGER_OWNER,
                "apply",
                DEBUG_NETWORK_APPLY_DESCRIPTOR,
                false
            )
            if (logEnabled) {
                println(
                    "[DebugNetworkInterceptorPlugin] inject variant=$variantName " +
                            "class=$className method=$methodName$methodDescriptor " +
                            "target=$owner.$name$descriptor"
                )
            }
        }

        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    private companion object {
        private const val OKHTTP_BUILDER_OWNER = "okhttp3/OkHttpClient\$Builder"
        private const val OKHTTP_BUILD_DESCRIPTOR = "()Lokhttp3/OkHttpClient;"
        private const val DEBUG_NETWORK_MANAGER_OWNER =
            "com/debugtoolkit/networkinterceptor/DebugNetworkInterceptorManager"
        private const val DEBUG_NETWORK_APPLY_DESCRIPTOR =
            "(Lokhttp3/OkHttpClient\$Builder;)Lokhttp3/OkHttpClient\$Builder;"
        private const val DEBUG_NETWORK_PROCESS_GUARD_OWNER =
            "com/debugtoolkit/networkinterceptor/DebugNetworkProcessGuard"
        private const val DEBUG_NETWORK_PROCESS_GUARD_DESCRIPTOR =
            "(Landroid/app/Application;)Z"
    }
}

private fun ClassData.isApplicationSubclass(): Boolean {
    return superClasses.any { superClass ->
        superClass == "android.app.Application" || superClass == "android/app/Application"
    }
}
