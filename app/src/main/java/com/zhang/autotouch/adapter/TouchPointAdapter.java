package com.zhang.autotouch.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.zhang.autotouch.BaiduManager;
import com.zhang.autotouch.R;
import com.zhang.autotouch.bean.TouchPoint;

import java.util.List;
import java.util.Set;

public class TouchPointAdapter extends RecyclerView.Adapter<TouchPointAdapter.TouchPointHolder> implements View.OnClickListener {

    private List<String> touchPointList;
    private OnItemClickListener onItemClickListener;
//    private int touchPosition = -1;

    public TouchPointAdapter() {
    }

    public void setTouchPointList(List<String> touchPointList) {
        this.touchPointList = touchPointList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TouchPointHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_touch_point, parent, false);
        view.setOnClickListener(this);
        return new TouchPointHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull TouchPointHolder holder, final int position) {
        holder.itemView.setTag(position);
//        TouchPoint touchPoint = getItem(position);
        holder.tvName.setText(touchPointList.get(position));
//        holder.tvOffset.setText("间隔(" + touchPoint.getDelay() + "s)");
        holder.tvOffset.setText("");
        holder.tvOffset.setVisibility(View.GONE);
//        holder.btStop.setVisibility(touchPosition == position ? View.VISIBLE : View.INVISIBLE);
        holder.btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BaiduManager.getInstance().deleteKey(touchPointList.get(position));
                touchPointList.remove(position);
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return touchPointList == null ? 0 : touchPointList.size();
    }

    public String getItem(int position) {
        return touchPointList.get(position);
    }

    @Override
    public void onClick(View v) {
        if (onItemClickListener != null) {
            int postion = (int) v.getTag();
//            TouchPoint touchPoint = getItem(postion);
            onItemClickListener.onItemClick(v, postion, null);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position, String key);
    }

    public static class TouchPointHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvOffset;
        Button btStop;

        public TouchPointHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvOffset = itemView.findViewById(R.id.tv_offset);
            btStop = itemView.findViewById(R.id.bt_stop);
        }
    }

}
