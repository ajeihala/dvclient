package com.dimonvideo.client.ui.forum;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.dimonvideo.client.Config;
import com.dimonvideo.client.MainActivity;
import com.dimonvideo.client.R;
import com.dimonvideo.client.adater.ForumPostsAdapter;
import com.dimonvideo.client.model.FeedForum;
import com.dimonvideo.client.util.NetworkUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.M)
public class PostsSearch extends AppCompatActivity  implements RecyclerView.OnScrollChangeListener, SwipeRefreshLayout.OnRefreshListener {

    private List<FeedForum> listFeed;
    public RecyclerView recyclerView;
    public RecyclerView.Adapter adapter;
    private RequestQueue requestQueue;
    private int requestCount = 1;
    private ProgressBar progressBar, ProgressBarBottom;
    String url = Config.FORUM_POSTS_URL;
    String tid = "1728146606";
    String t_name;
    SwipeRefreshLayout swipLayout;

    SharedPreferences sharedPrefs;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean is_dark = sharedPrefs.getBoolean("dvc_theme",false);
        final int auth_state = sharedPrefs.getInt("auth_state", 0);
        final String is_pm = sharedPrefs.getString("dvc_pm", "off");
        if (is_dark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        adjustFontScale( getResources().getConfiguration());
        setContentView(R.layout.comments_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getIntent()!=null) {
            tid = (String) getIntent().getSerializableExtra(Config.TAG_ID);
            t_name = (String) getIntent().getSerializableExtra(Config.TAG_STORY);
        }

        Objects.requireNonNull(getSupportActionBar()).setTitle(t_name);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        recyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setOnScrollChangeListener(this);

        listFeed = new ArrayList<>();
        requestQueue = Volley.newRequestQueue(this);
        progressBar = findViewById(R.id.progressbar);
        progressBar.setVisibility(View.VISIBLE);
        ProgressBarBottom = findViewById(R.id.ProgressBarBottom);
        ProgressBarBottom.setVisibility(View.GONE);
        // получение данных
        getData();
        adapter = new ForumPostsAdapter(listFeed, this);

        // разделитель позиций
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.divider)));
        recyclerView.addItemDecoration(dividerItemDecoration);

        recyclerView.setAdapter(adapter);
        swipLayout = findViewById(R.id.swipe_layout);
        swipLayout.setOnRefreshListener(this);

        LinearLayout post_layout = findViewById(R.id.post);
        if (auth_state > 0) post_layout.setVisibility(View.VISIBLE);
        // отправка ответа
        Button btnSend = findViewById(R.id.btnSend);
        EditText textInput = findViewById(R.id.textInput);

        btnSend.setOnClickListener(v -> {
            NetworkUtils.sendPm(this, Integer.parseInt(tid), textInput.getText().toString(), 2, null);
            textInput.getText().clear();
        });

        // open PM
        FloatingActionButton fab = findViewById(R.id.fab);
        if ((is_pm.equals("off")) || (auth_state != 1)) fab.setVisibility(View.GONE);
        fab.setOnClickListener(view -> {
            Intent notificationIntent = new Intent(getBaseContext(), MainActivity.class);
            notificationIntent.putExtra("action", "PmFragment");
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(notificationIntent);

        });
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(receiver, new IntentFilter("com.dimonvideo.client.NEW_PM"));
        if (auth_state > 0) {

            // обновляем счетчик лс
            @SuppressLint("StaticFieldLeak")
            class AsyncCountPm extends AsyncTask<String, String, String> {
                SharedPreferences sharedPrefs;
                private WeakReference<Context> contextRef;

                public AsyncCountPm(Context context) {
                    this.contextRef = new WeakReference<>(context);
                }

                @Override
                protected String doInBackground(String... params) {
                    Context context = contextRef.get();
                    if (context != null) {
                        try {
                            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                            // check is logged
                            final String password = sharedPrefs.getString("dvc_password", "null");
                            View view = ((Activity) context).getWindow().getDecorView().getRootView();

                            NetworkUtils.checkPassword(context, view, password);

                            return null;
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(String result) {
                    super.onPostExecute(result);
                    final int pm_unread = sharedPrefs.getInt("pm_unread", 0);
                    if (pm_unread > 0) {
                        TextView fab_badge = findViewById(R.id.fab_badge);
                        fab_badge.setVisibility(View.VISIBLE);
                        fab_badge.setText(String.valueOf(pm_unread));
                    }
                }
            }
            AsyncCountPm task = new AsyncCountPm(this);
            task.execute();

        }
    }

    // запрос к серверу апи
    private JsonArrayRequest getDataFromServer(int requestCount) {
        try {
            t_name = URLEncoder.encode(t_name, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return new JsonArrayRequest(url + requestCount + "&id=" + tid + "&story=" + t_name,
                response -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                    for (int i = 0; i < response.length(); i++) {
                        FeedForum jsonFeed = new FeedForum();
                        JSONObject json;
                        try {
                            json = response.getJSONObject(i);
                            jsonFeed.setImageUrl(json.getString(Config.TAG_IMAGE_URL));
                            jsonFeed.setId(json.getInt(Config.TAG_ID));
                            jsonFeed.setLast_poster_name(json.getString(Config.TAG_LAST_POSTER_NAME));
                            jsonFeed.setUser(json.getString(Config.TAG_USER));
                            jsonFeed.setTitle(json.getString(Config.TAG_TITLE));
                            jsonFeed.setText(json.getString(Config.TAG_TEXT));
                            jsonFeed.setCategory(json.getString(Config.TAG_CATEGORY));
                            jsonFeed.setDate(json.getString(Config.TAG_DATE));
                            jsonFeed.setState(json.getString(Config.TAG_STATE));
                            jsonFeed.setPinned(json.getString(Config.TAG_PINNED));
                            jsonFeed.setComments(json.getInt(Config.TAG_COMMENTS));
                            jsonFeed.setTime(json.getLong(Config.TAG_TIME));
                            jsonFeed.setHits(json.getInt(Config.TAG_HITS));
                            jsonFeed.setNewtopic(json.getInt(Config.TAG_NEW_TOPIC));
                            jsonFeed.setTopic_id(json.getInt(Config.TAG_TOPIC_ID));
                            jsonFeed.setMin(json.getInt(Config.TAG_MIN));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        listFeed.add(jsonFeed);
                    }
                    adapter.notifyDataSetChanged();

                },
                error -> {
                    progressBar.setVisibility(View.GONE);
                    ProgressBarBottom.setVisibility(View.GONE);
                });
    }

    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String str = intent.getStringExtra("count");
                TextView fab_badge = findViewById(R.id.fab_badge);
                fab_badge.setVisibility(View.VISIBLE);
                fab_badge.setText(str);
                if ((str == null) || (str.equals("0"))) fab_badge.setVisibility(View.GONE);
            }
        }
    };
    // получение данных и увеличение номера страницы
    private void getData() {
        ProgressBarBottom.setVisibility(View.VISIBLE);
        requestQueue.add(getDataFromServer(requestCount));
        requestCount++;
    }

    // опредление последнего элемента
    private boolean isLastItemDisplaying(RecyclerView recyclerView) {
        if (Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() != 0) {
            int lastVisibleItemPosition = ((LinearLayoutManager) Objects.requireNonNull(recyclerView.getLayoutManager())).findLastCompletelyVisibleItemPosition();
            return lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition == recyclerView.getAdapter().getItemCount() - 1;
        }
        return false;
    }

    // получение следующей страницы при скролле
    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
        if (isLastItemDisplaying(recyclerView)) {
            getData();
        }
    }

    // обновление
    @Override
    public void onRefresh() {
        requestCount = 1;
        recreate();
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(receiver);
        } catch (Throwable ignored) {
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_topics, menu);
        return true;
    }
    // toolbar main arrow
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        // main arrow
        if (id == android.R.id.home) {
            onBackPressed();
        }
        // refresh
        if (id == R.id.action_refresh) {
            recreate();
        }
        // open page
        if (id == R.id.action_open) {

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Config.BASE_URL+"/forum/topic_"+tid));
            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }
        // share
        if (id == R.id.action_share) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, Uri.parse(Config.BASE_URL+"/forum/topic_"+tid));
            sendIntent.setType("text/plain");
            Intent shareIntent = Intent.createChooser(sendIntent, t_name);
            try {
                this.startActivity(shareIntent);
            } catch (Throwable ignored) {
            }
        }

        // feedback
        if (id == R.id.action_feedback) {

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.fromParts("mailto", getString(R.string.app_mail), null));
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " Feedback");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            try {
                startActivity(intent);
            } catch (Throwable ignored) {
            }
        }

        // donate
        if (id == R.id.action_donate) {

            String url = Config.BASE_URL + "/reklama.php";

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    url));

            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }

        // votes
        if (id == R.id.action_vote) {

            String url = Config.BASE_URL + "/votes";

            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                    url));

            try {
                startActivity(browserIntent);
            } catch (Throwable ignored) {
            }
        }

        return super.onOptionsItemSelected(item);
    }

    // scale fonts
    private void adjustFontScale(Configuration configuration) {
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        configuration.fontScale = Float.parseFloat(sharedPrefs.getString("dvc_scale","1.0f"));
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        assert wm != null;
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.scaledDensity = configuration.fontScale * metrics.density;
        getBaseContext().getResources().updateConfiguration(configuration, metrics);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}