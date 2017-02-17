package de.eknoes.inofficialgolem;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link ArticleFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ArticleFragment extends Fragment {
    static final String ARTICLE_URL = "de.eknoes.inofficialgolem.ARTICLE_URL";
    static final String FORCE_WEBVIEW = "de.eknoes.inofficialgolem.FORCE_WEBVIEW";

    private static final String TAG = "ArticleFragment";
    private String url;
    private boolean forceWebview;
    private WebView webView;
    private ProgressBar progress;

    private Article article;
    private loadArticleTask mTask;

    public ArticleFragment() {
        super();
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param articleUrl   URL of the article or Page to open
     * @param forceWebview Force webView even if offline version is available
     * @return A new instance of fragment ArticleFragment.
     */
    static ArticleFragment newInstance(String articleUrl, boolean forceWebview) {
        ArticleFragment fragment = new ArticleFragment();
        Bundle args = new Bundle();
        args.putString(ARTICLE_URL, articleUrl);
        args.putBoolean(FORCE_WEBVIEW, forceWebview);
        fragment.setArguments(args);
        return fragment;
    }

    void updateArticle(String url, boolean forceWebview) {
        this.url = url;
        this.forceWebview = forceWebview;
        mTask = new loadArticleTask();
        mTask.execute();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(FORCE_WEBVIEW, forceWebview);
        outState.putString(ARTICLE_URL, url);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(ARTICLE_URL);
            forceWebview = getArguments().getBoolean(FORCE_WEBVIEW);
        } else if (savedInstanceState != null) {
            url = savedInstanceState.getString(ARTICLE_URL);
            forceWebview = savedInstanceState.getBoolean(FORCE_WEBVIEW);
        }

        if(webView != null) {
            webView.setWebViewClient(new GolemWebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);

                    progress.setVisibility(View.GONE);
                }
            });
            webView.getSettings().setJavaScriptEnabled(true);
        }

        if (url != null) {
            mTask = new loadArticleTask();
            mTask.execute();
        } else {
            Log.d(TAG, "onActivityCreated: URL is Null, do not fetch article");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        Log.d(TAG, "onCreateView: Inflating Fragment layout");
        View v = inflater.inflate(R.layout.fragment_article, container, false);
        webView = (WebView) v.findViewById(R.id.articleWebView);
        progress = (ProgressBar) v.findViewById(R.id.articleProgress);

        Button mailBtn = (Button) v.findViewById(R.id.mailBtn);
        Button storeBtn = (Button) v.findViewById(R.id.storeBtn);

        storeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String packageName = getContext().getPackageName();
                Intent playStoreIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + packageName));
                playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                } else {
                    playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                }

                try {
                    startActivity(playStoreIntent);
                } catch (Exception e) {
                    Intent webIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        webIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                    } else {
                        webIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    }

                    startActivity(webIntent);
                }
            }
        });

        mailBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "projekte@eknoes.de", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Inofficial golem.de Reader");
                startActivity(Intent.createChooser(emailIntent, getResources().getString(R.string.sendMail)));
            }
        });
        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
        setHasOptionsMenu(false);

    }

    @Override
    public void onResume() {
        super.onResume();
        calculateSettings();
        setHasOptionsMenu(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(mTask != null) {
            mTask.cancel(false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_webview, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_open) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW);
            if (url != null) {
                webIntent.setData(Uri.parse(url));
                startActivity(webIntent);
            }
        } else if (id == R.id.action_share_article) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            String link = null;
            if (article.getUrl() != null) {
                link = article.getUrl();
            } else if (url != null) {
                link = url;
            }

            if (link != null) {
                shareIntent.putExtra(Intent.EXTRA_TEXT, article.getSubheadline() + ": " + article.getTitle() + " - " + link);
                shareIntent.setType("text/plain");
                startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.choose_share_article)));
            }
        }

        return super.onOptionsItemSelected(item);
    }


    private void calculateSettings() {
        WebSettings settings = webView.getSettings();
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setDefaultTextEncodingName("utf-8");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            int value;
            switch (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("text_zoom", "normal")) {
                case "smaller":
                    value = 90;
                    break;
                case "bigger":
                    value = 110;
                    break;
                default:
                    value = 100;
            }

            settings.setTextZoom(value);
        } else {
            WebSettings.TextSize value;
            switch (PreferenceManager.getDefaultSharedPreferences(getContext()).getString("text_zoom", "normal")) {
                case "smaller":
                    value = WebSettings.TextSize.SMALLER;
                    break;
                case "larger":
                    value = WebSettings.TextSize.LARGER;
                    break;
                default:
                    value = WebSettings.TextSize.NORMAL;
            }

            settings.setTextSize(value);
        }

    }

    private class loadArticleTask extends AsyncTask<Void, Void, Void> {

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */

        @Override
        protected Void doInBackground(Void... params) {
            if (webView != null) {

                FeedReaderDbHelper dbHelper = FeedReaderDbHelper.getInstance(getContext());
                SQLiteDatabase db = dbHelper.getReadableDatabase();

                if (url != null) {
                    String[] columns = {
                            FeedReaderContract.Article.COLUMN_NAME_ID,
                            FeedReaderContract.Article.COLUMN_NAME_TITLE,
                            FeedReaderContract.Article.COLUMN_NAME_SUBHEADING,
                            FeedReaderContract.Article.COLUMN_NAME_URL,
                            FeedReaderContract.Article.COLUMN_NAME_OFFLINE,
                            FeedReaderContract.Article.COLUMN_NAME_FULLTEXT,
                    };
                    Cursor cursor = db.query(
                            FeedReaderContract.Article.TABLE_NAME,
                            columns,
                            "url=\"" + url + "\"",
                            null,
                            null,
                            null,
                            null);
                    if (cursor.moveToFirst()) {
                        article = new Article();
                        article.setId(cursor.getInt(cursor.getColumnIndex(FeedReaderContract.Article.COLUMN_NAME_ID)));
                        article.setTitle(cursor.getString(cursor.getColumnIndex(FeedReaderContract.Article.COLUMN_NAME_TITLE)));
                        article.setSubheadline(cursor.getString(cursor.getColumnIndex(FeedReaderContract.Article.COLUMN_NAME_SUBHEADING)));
                        article.setUrl(cursor.getString(cursor.getColumnIndex(FeedReaderContract.Article.COLUMN_NAME_URL)));
                        article.setOffline(cursor.getInt(cursor.getColumnIndex(FeedReaderContract.Article.COLUMN_NAME_OFFLINE)) == 1);
                        article.setFulltext(cursor.getString(cursor.getColumnIndex(FeedReaderContract.Article.COLUMN_NAME_FULLTEXT)));
                    }


                    cursor.close();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void vVoid) {
            if(isCancelled()) {
                return;
            }
            if (article == null || !article.isOffline() || forceWebview ) {
                Context c = getContext();
                if (c != null) {
                    ConnectivityManager connMgr = (ConnectivityManager)
                            getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.isConnected()) {
                        progress.setVisibility(View.VISIBLE);
                        progress.setEnabled(true);
                        progress.setIndeterminate(true);
                        webView.loadUrl(url);
                    } else {
                        webView.loadData(getResources().getString(R.string.err_no_network), "text/html; charset=utf-8", "UTF-8");
                    }
                } else {
                    webView.loadUrl(url);
                }
            } else {
                webView.loadData(article.getFulltext(), "text/html; charset=utf-8", "UTF-8");
            }
        }
    }

}