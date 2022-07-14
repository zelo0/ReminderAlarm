package com.example.reminderalarm.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reminderalarm.R;
import com.example.reminderalarm.data.EventCoreInfo;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private List<EventCoreInfo> dataSet;

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleTextView;
        private final TextView dtStartTextView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            titleTextView = (TextView) view.findViewById(R.id.eventTitle);
            dtStartTextView = (TextView) view.findViewById(R.id.eventStartTime);
        }

        public TextView getTitleTextView() {
            return titleTextView;
        }

        public TextView getDtStartTextView() {
            return dtStartTextView;
        }
    }

    public EventAdapter(List<EventCoreInfo> dataSet) {
        this.dataSet = dataSet;
    }


    @NonNull
    @Override
    public EventAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.event_item, viewGroup, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getDtStartTextView().setText(dataSet.get(position).getTimeString());
        viewHolder.getTitleTextView().setText(dataSet.get(position).getTitle());
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }
}
