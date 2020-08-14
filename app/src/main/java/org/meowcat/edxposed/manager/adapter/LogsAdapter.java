package org.meowcat.edxposed.manager.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.meowcat.edxposed.manager.R;

import java.util.ArrayList;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.ViewHolder> {

    protected final Context context;
    private final RecyclerView recyclerView;
    private ArrayList<String> logs = new ArrayList<>();

    public LogsAdapter(Context context, RecyclerView recyclerView) {
        this.context = context;
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TextView view = holder.textView;
        view.setText(logs.get(position));
        view.measure(0, 0);
        int desiredWidth = view.getMeasuredWidth();
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = desiredWidth;
        if (recyclerView.getWidth() < desiredWidth) {
            recyclerView.requestLayout();
        }
    }

    @Override
    public int getItemCount() {
        return logs.size();
    }

    void setLogs(ArrayList<String> log) {
        logs.clear();
        logs.addAll(log);
        notifyDataSetChanged();
    }

    public void setEmpty() {
        logs.clear();
        logs.add(context.getString(R.string.log_is_empty));
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        ViewHolder(View view) {
            super(view);
            textView = view.findViewById(R.id.log);
        }
    }
}