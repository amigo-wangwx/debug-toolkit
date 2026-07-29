package com.debugtoolkit

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.debugtoolkit.networkinterceptor.DebugOperationLog

class DebugButtonAdapter(
    private val context: Context,
    private val items: List<DebugAction>
) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): DebugAction = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            // 创建新视图
            view = LayoutInflater.from(context).inflate(R.layout.item_debug_button, parent, false)
            holder = ViewHolder(view)
            view.tag = holder
        } else {
            // 重用视图
            view = convertView
            holder = view.tag as ViewHolder
        }

        val item = getItem(position)

        // 设置按钮文本和背景色
        holder.button.text = item.title
        holder.button.setBackgroundColor(Color.parseColor(item.backgroundColor))
        holder.button.setOnClickListener {
            DebugOperationLog.record(
                category = item.group,
                action = item.id,
                message = "click title=${item.title}"
            )
            item.onClick()
        }

        return view
    }

    // ViewHolder模式优化性能
    private class ViewHolder(view: View) {
        val button: Button = view.findViewById(R.id.btn_debug)
    }
}
