package com.example.eyesthetic;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class ClosetAdapter extends RecyclerView.Adapter<ClosetAdapter.ViewHolder> {
    public interface OnItemClickListener {
        void onItemClick(ClosetItem item);
    }

    private List<ClosetItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public ClosetAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<ClosetItem> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameView;

        public ViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.clothingImage);
            nameView = view.findViewById(R.id.clothingName);
        }

        public void bind(ClosetItem item, OnItemClickListener listener) {
            Log.d("AdapterBind", "Binding: " + item.getName());

            nameView.setText(item.getName());
            Glide.with(imageView.getContext()).load(item.getImageUrl()).into(imageView);
            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.closet_item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
