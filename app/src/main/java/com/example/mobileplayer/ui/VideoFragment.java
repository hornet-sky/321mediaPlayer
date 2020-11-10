package com.example.mobileplayer.ui;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.mobileplayer.R;
import com.example.mobileplayer.component.SystemVideoPlayer;
import com.example.mobileplayer.entity.MediaItem;
import com.example.mobileplayer.utils.MediaUtils;

import java.util.ArrayList;
import java.util.List;

public class VideoFragment extends BaseFragment {
    private Context context;
    private TextView noDataMsgTextView;
    private ProgressBar pb;
    private RecyclerView rv;
    private MyAdapter adapter;
    public VideoFragment(Context context) {
        super(context);
        this.context = context;
        Log.w("myTag", "VideoFragment.constructor()");
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_video;
    }

    @Override
    protected void initView(FrameLayout rootView) {
        rv = ((RecyclerView) rootView.getChildAt(0));
        noDataMsgTextView = ((TextView) rootView.getChildAt(1));
        pb = ((ProgressBar) rootView.getChildAt(2));
    }

    @Override
    protected void initData() {
        Log.w("myTag", "VideoFragment.initData()");
        adapter = new MyAdapter();
        rv.setAdapter(adapter);
        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));

        new MyTask().execute();
    }

    class MyTask extends AsyncTask<Void, Void, List<MediaItem>> {
        @Override
        protected void onPreExecute() {
            pb.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<MediaItem> doInBackground(Void... params) {
            return MediaUtils.listAllExternalVideos(context);
        }
        @Override
        protected void onPostExecute(List<MediaItem> mediaItems) {
            Log.w("myTag", "MyTask.onPostExecute [mediaItems.size=" + mediaItems.size() + "]");
            pb.setVisibility(View.GONE);
            adapter.submitList(mediaItems);
            if(mediaItems.isEmpty()) {
                noDataMsgTextView.setVisibility(View.VISIBLE);
            }
        }
    }

    static class MyAdapter extends ListAdapter<MediaItem, MyViewHolder> {
        protected MyAdapter() {
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
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.rv_video_item_layout, parent, false);
            MyViewHolder holder = new MyViewHolder(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int position = holder.getAdapterPosition();
                    MediaItem item = getItem(position);
                    /* 1、使用外部的播放器
                    Intent intent = new Intent();
                    intent.setDataAndType(item.getUri(), "video/*");
                    parent.getContext().startActivity(intent);
                    */
                    /* 2、使用VideoView组件
                    Intent intent = new Intent(parent.getContext(), SystemVideoPlayer.class);
                    intent.setData(item.getUri());
                    parent.getContext().startActivity(intent);
                     */
                    /* 2、传整个列表过去 */
                    Intent intent = new Intent(parent.getContext(), SystemVideoPlayer.class);
                    intent.putParcelableArrayListExtra("data", new ArrayList<>(getCurrentList()));
                    intent.putExtra("itemIndex", position);
                    parent.getContext().startActivity(intent);
                }
            });
            return holder;
        }
        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            MediaItem item = getItem(position);
            holder.nameTextView.setText(item.getName());
            holder.durationTextView.setText(MediaUtils.formatDuration(item.getDuration()));
            holder.sizeTextView.setText(String.valueOf(Formatter.formatFileSize(holder.itemView.getContext(), item.getSize())));
        }
    }

    static class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView nameTextView;
        private TextView durationTextView;
        private TextView sizeTextView;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            durationTextView = itemView.findViewById(R.id.titleTextView);
            sizeTextView = itemView.findViewById(R.id.sizeTextView);
        }
    }
}