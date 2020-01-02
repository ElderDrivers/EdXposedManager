package com.solohsu.android.edxp.manager.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.solohsu.android.edxp.manager.R;
import com.github.coxylicacid.mdwidgets.dialog.MD2Dialog;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import de.robv.android.xposed.installer.XposedApp;
import de.robv.android.xposed.installer.util.ThemeUtil;

public class PlansAdapter extends RecyclerView.Adapter<PlansAdapter.ViewHolder> {

    private List<Plan> plans;
    private Context context;

    class ViewHolder extends RecyclerView.ViewHolder {
        View container;
        LinearLayout planBG;
        TextView plan;
        TextView progress;
        TextView date;
        TextView alreadyUpdate;

        ViewHolder(View view) {
            super(view);
            container = view;
            plan = view.findViewById(R.id.plan);
            planBG = view.findViewById(R.id.plan_bg);
            progress = view.findViewById(R.id.progress);
            date = view.findViewById(R.id.date);
            alreadyUpdate = view.findViewById(R.id.already_update);
        }
    }

    // Gson解析要用的
    public class Plans {
        public int finished; // 已完成的计划数量
        public int unfinished; // 未完成的数量
        public List<Plan> plans; // 计划数组
    }

    public class Plan {
        public String plan; // 计划名称
        public float progress; // 计划进度
        public boolean status; // 计划完成状态
        public long date; // 时间戳
    }

    public PlansAdapter(Context context, List<Plan> plans) {
        this.plans = plans;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_plan, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.alreadyUpdate.setTextColor((XposedApp.isNightMode() ? 0xFFFFFFFF : MD2Dialog.COLOR_SUCCESSFUL));
        return holder;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Plan p = plans.get(position);
        holder.plan.setText(p.plan);
        holder.date.setText(p.date != 0 ? stampToDate(p.date) : context.getString(R.string.unfinished));
        holder.planBG.setBackgroundColor(Color.argb((int) (255 * p.progress), 24, 113, 239));
        holder.progress.setText(context.getString(R.string.plan_progress) + (int) (p.progress * 100) + "%");
        holder.alreadyUpdate.setVisibility(p.status ? View.VISIBLE : View.GONE);

        if (p.status) {
            p.progress = 1;
            holder.planBG.setBackgroundColor(Color.rgb(24, 113, 239));
            holder.progress.setText(R.string.progress_hundred_percent);
        }

        if (p.progress > 0.35) {
            holder.plan.setTextColor(0xFFFFFFFF);
            holder.progress.setTextColor(0xFFFFFFFF);
            holder.date.setTextColor(0xFFFFFFFF);
        }
    }

    @Override
    public int getItemCount() {
        return plans.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static String stampToDate(Long tsp, String... format) {
        SimpleDateFormat sdf;
        if (format.length < 1) {
            sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss", Locale.getDefault());
        } else {
            sdf = new SimpleDateFormat(format[0], Locale.getDefault());
        }
        return sdf.format(tsp * 1000L);
    }

}
