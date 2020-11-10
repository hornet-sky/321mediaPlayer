package com.example.mobileplayer.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aspsine.swipetoloadlayout.OnLoadMoreListener;
import com.aspsine.swipetoloadlayout.OnRefreshListener;
import com.aspsine.swipetoloadlayout.SwipeToLoadLayout;
import com.bumptech.glide.Glide;
import com.example.mobileplayer.R;
import com.example.mobileplayer.component.LoadMoreFooterView;
import com.example.mobileplayer.component.RefreshHeaderView;
import com.example.mobileplayer.component.SystemVideoPlayer;
import com.example.mobileplayer.entity.MediaItem;
import com.example.mobileplayer.utils.MediaUtils;
import com.example.mobileplayer.utils.SizeUtils;

import org.xutils.view.annotation.ContentView;
import org.xutils.view.annotation.ViewInject;
import org.xutils.x;

import java.util.ArrayList;
import java.util.List;

@ContentView(R.layout.fragment_net_video)
public class NetVideoFragment extends BaseFragment {
    private Context context;
    @ViewInject(R.id.swipeToLoadLayout)
    private SwipeToLoadLayout swipeToLoadLayout;
    @ViewInject(R.id.swipe_refresh_header)
    private RefreshHeaderView refreshHeaderView;
    @ViewInject(R.id.swipe_target)
    private RecyclerView rv;
    @ViewInject(R.id.swipe_load_more_footer)
    private LoadMoreFooterView loadMoreFooterView;

    @ViewInject(R.id.msgTextView)
    private TextView msgTextView;
    @ViewInject(R.id.progressBar)
    private ProgressBar pb;
    private List<MediaItem> mediaItems;
    private MyAdapter adapter;
    private int pageNum = 1, pageSize = 50;
    private String type = "videoType1";
    private MediaUtils.NetVideosLoadCallback netVideosLoadCallback = new MediaUtils.NetVideosLoadCallback() {
        @Override
        public void beforeRequest() {
        }
        @Override
        public void onSuccess(List<MediaItem> items) {
            Log.w("myTag", "NetVideosLoadCallback.onSuccess - " + items.size());
            if(pageNum == 1 && !mediaItems.isEmpty()) { // 下拉刷新了
                mediaItems.clear();
                adapter.notifyDataSetChanged();
                loadMoreFooterView.setNoMoreData(false);
            }
            mediaItems.addAll(items);
            if(mediaItems.size() == 0) {
                msgTextView.setText("暂无数据");
                msgTextView.setVisibility(View.VISIBLE);
                return;
            }
            adapter.notifyItemRangeChanged(mediaItems.size() - items.size(), items.size());
            if(items.isEmpty()) {
                loadMoreFooterView.setNoMoreData(true);
            }
            msgTextView.setVisibility(View.GONE);
        }
        @Override
        public void onError(Throwable ex) {
            Log.e("myTag", "NetVideosLoadCallback.onError", ex);
            mediaItems.clear();
            adapter.notifyDataSetChanged();
            msgTextView.setText("加载网络数据失败");
            msgTextView.setVisibility(View.VISIBLE);
        }
        @Override
        public void onFinished() {
            if(pb.getVisibility() == View.VISIBLE) {
                pb.setVisibility(View.GONE);
            }
            if(swipeToLoadLayout.isRefreshing()) {
                swipeToLoadLayout.setRefreshing(false);
            }
            if(swipeToLoadLayout.isLoadingMore()) {
                swipeToLoadLayout.setLoadingMore(false);
            }
        }
    };
    public NetVideoFragment(Context context) {
        super(context);
        this.context = context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return x.view().inject(this, inflater, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mediaItems = new ArrayList<>();
        adapter = new NetVideoFragment.MyAdapter(mediaItems);
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        MediaUtils.listNetVideos(pageNum, pageSize, type, netVideosLoadCallback);

        swipeToLoadLayout.setRefreshTriggerOffset(SizeUtils.getInstance(context).dip2px(80F));
        swipeToLoadLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                pageNum = 1;
                type = "换一种视频分类";
                MediaUtils.listNetVideos(pageNum, pageSize, type, netVideosLoadCallback);
            }
        });
        swipeToLoadLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                MediaUtils.listNetVideos(++pageNum, pageSize, type, netVideosLoadCallback);
            }
        });
    }

    static class MyAdapter extends ListAdapter<MediaItem, NetVideoFragment.MyViewHolder> {
        private List<MediaItem> mediaItems;
        protected MyAdapter(List<MediaItem> mediaItems) {
            super(new DiffUtil.ItemCallback<MediaItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull MediaItem oldItem, @NonNull MediaItem newItem) {
                    return oldItem == newItem;
                }
                @Override
                public boolean areContentsTheSame(@NonNull MediaItem oldItem, @NonNull MediaItem newItem) {
                    return oldItem.getUri().equals(newItem.getUri());
                }
            });
            this.mediaItems = mediaItems;
            submitList(mediaItems);
        }

        @NonNull
        @Override
        public NetVideoFragment.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_net_video_item_layout, parent, false);
            NetVideoFragment.MyViewHolder holder = new NetVideoFragment.MyViewHolder(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = holder.getAdapterPosition();
                    MediaItem item = getItem(position);
                    /* 传整个列表过去 */
                    Intent intent = new Intent(parent.getContext(), SystemVideoPlayer.class);
                    intent.putParcelableArrayListExtra("data", (ArrayList) mediaItems);
                    intent.putExtra("itemIndex", position);
                    parent.getContext().startActivity(intent);
                }
            });
            return holder;
        }
        @Override
        public void onBindViewHolder(@NonNull NetVideoFragment.MyViewHolder holder, int position) {
            MediaItem item = getItem(position);
            holder.nameTextView.setText((position + 1) + "、" + item.getName());
            holder.titleTextView.setText(item.getTitle());
            Glide.with(holder.itemView)
                    .load(item.getCoverImg())
                    .centerCrop()
                    .thumbnail(Glide.with(holder.itemView).load(R.drawable.loading))
                    .error(R.drawable.pictures_no) // 设置了thumbnail后error就没效果了
                    .into(holder.coverImageView);
        }
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView titleTextView;
        private ImageView coverImageView;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            coverImageView = itemView.findViewById(R.id.coverImageView);
        }
    }
}