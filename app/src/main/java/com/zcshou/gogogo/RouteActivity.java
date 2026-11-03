package com.zcshou.gogogo;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textview.MaterialTextView;
import com.zcshou.database.DataBaseRoute;
import com.zcshou.utils.GoUtils;

import java.util.ArrayList;
import java.util.List;

public class RouteActivity extends BaseActivity {
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private MaterialTextView emptyText;
    private DataBaseRoute database;
    private final List<DataBaseRoute.RouteInfo> routes = new ArrayList<>();
    private RouteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!LicenseManager.requireAuthorization(this)) {
            return;
        }

        setContentView(R.layout.activity_route);
        setTitle("路线管理");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        database = new DataBaseRoute(this);
        refreshLayout = findViewById(R.id.route_swipe_refresh);
        recyclerView = findViewById(R.id.route_recycler);
        emptyState = findViewById(R.id.route_empty_state);
        emptyText = findViewById(R.id.route_no_data);
        ExtendedFloatingActionButton importButton = findViewById(R.id.fab_import);

        adapter = new RouteAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        refreshLayout.setOnRefreshListener(this::refreshRoutes);
        importButton.setOnClickListener(view -> showImportDialog());
        refreshRoutes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRoutes();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshRoutes() {
        routes.clear();
        List<DataBaseRoute.RouteInfo> savedRoutes = database.getAllRoutes();
        if (savedRoutes != null) {
            routes.addAll(savedRoutes);
        }
        adapter.notifyDataSetChanged();
        refreshLayout.setRefreshing(false);

        boolean empty = routes.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            emptyText.setText("暂无路线\n可去绘制新路线或从剪贴板导入");
        }
    }

    private void showImportDialog() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()
                || clipboard.getPrimaryClip() == null
                || clipboard.getPrimaryClip().getItemCount() == 0) {
            GoUtils.DisplayToast(this, "剪贴板为空，请先复制路线数据");
            return;
        }
        CharSequence clipboardText = clipboard.getPrimaryClip().getItemAt(0).getText();
        if (TextUtils.isEmpty(clipboardText)) {
            GoUtils.DisplayToast(this, "剪贴板内容无效");
            return;
        }

        EditText input = new EditText(this);
        input.setHint("请输入路线名称");
        new AlertDialog.Builder(this)
                .setTitle("导入路线")
                .setMessage("检测到剪贴板有数据，请输入名称保存：")
                .setView(input)
                .setPositiveButton("保存", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        name = "未命名路线_" + System.currentTimeMillis();
                    }
                    database.saveRoute(name, clipboardText.toString());
                    GoUtils.DisplayToast(this, "导入成功");
                    refreshRoutes();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showActionDialog(DataBaseRoute.RouteInfo route) {
        String[] options = {"导出到剪贴板", "删除路线"};
        new AlertDialog.Builder(this)
                .setTitle("操作: " + route.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        exportRoute(route);
                    } else {
                        confirmDelete(route);
                    }
                })
                .show();
    }

    private void exportRoute(DataBaseRoute.RouteInfo route) {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("RouteData", route.pointsJson));
            GoUtils.DisplayToast(this, "路线数据已复制到剪贴板");
        }
    }

    private void confirmDelete(DataBaseRoute.RouteInfo route) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除路线 \"" + route.name + "\" 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    database.deleteRoute(route.id);
                    GoUtils.DisplayToast(this, "已删除");
                    refreshRoutes();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private final class RouteAdapter extends RecyclerView.Adapter<RouteViewHolder> {
        @NonNull
        @Override
        public RouteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_route_card, parent, false);
            return new RouteViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RouteViewHolder holder, int position) {
            holder.title.setText(routes.get(position).name);
            holder.itemView.setOnClickListener(view ->
                    showActionDialog(routes.get(holder.getBindingAdapterPosition())));
        }

        @Override
        public int getItemCount() {
            return routes.size();
        }
    }

    private static final class RouteViewHolder extends RecyclerView.ViewHolder {
        private final MaterialTextView title;

        RouteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.route_item_title);
        }
    }
}
