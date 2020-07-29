package com.dimonvideo.client.adater;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.dimonvideo.client.R;
import com.dimonvideo.client.model.Feed;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.util.ButtonsActions;
import com.dimonvideo.client.util.NetworkUtils;

import org.sufficientlysecure.htmltextview.HtmlHttpImageGetter;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.Calendar;
import java.util.List;

public class ForumPostsAdapter extends RecyclerView.Adapter<ForumPostsAdapter.ViewHolder> {

    private Context context;

    //List to store all
    List<FeedForum> jsonFeed;

    //Constructor of this class
    public ForumPostsAdapter(List<FeedForum> jsonFeed, Context context){
        super();
        //Getting all feed
        this.jsonFeed = jsonFeed;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_row_posts, parent, false);


        return new ViewHolder(v);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {

        //Getting the particular item from the list
        final FeedForum Feed =  jsonFeed.get(position);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        holder.status_logo.setImageResource(R.drawable.ic_status_gray);

        try {
            if (Feed.getTime() > cal.getTimeInMillis() / 1000L) holder.status_logo.setImageResource(R.drawable.ic_status_green);
            } catch (Throwable ignored) {

        }
        Glide.with(context).load(Feed.getImageUrl()).apply(RequestOptions.circleCropTransform()).into(holder.imageView);
        holder.textViewTitle.setText(Feed.getTitle());
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String password = sharedPrefs.getString("dvc_password", "null");

        if ((Feed.getNewtopic() == 1) && (!password.equals("null"))) holder.post_layout.setVisibility(View.VISIBLE);

        holder.btnSend.setOnClickListener(v -> {
            NetworkUtils.sendPm(context, Feed.getTopic_id(), holder.textInput.getText().toString(), 2);
            holder.post_layout.setVisibility(View.GONE);
        });

        holder.textViewText.setHtml(Feed.getText(), new HtmlHttpImageGetter(holder.textViewText));


        holder.textViewDate.setText(Feed.getDate());
        holder.textViewNames.setText(Feed.getLast_poster_name());
        holder.textViewCategory.setText(Feed.getCategory());
        holder.textViewComments.setText(String.valueOf(Feed.getComments()));
        holder.textViewComments.setVisibility(View.VISIBLE);
        holder.rating_logo.setVisibility(View.VISIBLE);
        if (Feed.getComments() == 0) {
            holder.textViewComments.setVisibility(View.INVISIBLE);
            holder.rating_logo.setVisibility(View.INVISIBLE);
        }
        holder.textViewHits.setText(String.valueOf(Feed.getHits()));


    }

    @Override
    public int getItemCount() {
        return jsonFeed.size();
    }

    // swipe to remove favorites
    public void addPost(int position) {
        final FeedForum Feed =  jsonFeed.get(position);
        notifyDataSetChanged();
      //  ButtonsActions.add_post(context, Feed.getId(), 2);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        //Views
        public TextView textViewTitle, textViewDate, textViewComments, textViewCategory, textViewHits, textViewNames;
        public ImageView rating_logo, status_logo, imageView;
        public HtmlTextView textViewText;
        public LinearLayout post_layout;
        public Button btnSend;
        public EditText textInput;

        //Initializing Views
        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnail);
            rating_logo = itemView.findViewById(R.id.rating_logo);
            status_logo = itemView.findViewById(R.id.status);
            textViewNames = itemView.findViewById(R.id.title);
            textViewText = itemView.findViewById(R.id.listtext);
            textViewDate = itemView.findViewById(R.id.date);
            textViewComments = itemView.findViewById(R.id.rating);
            textViewCategory = itemView.findViewById(R.id.category);
            textViewHits = itemView.findViewById(R.id.views_count);
            textViewTitle = itemView.findViewById(R.id.names);
            post_layout = itemView.findViewById(R.id.post);
            btnSend = itemView.findViewById(R.id.btnSend);
            textInput = itemView.findViewById(R.id.textInput);

        }

    }
}
