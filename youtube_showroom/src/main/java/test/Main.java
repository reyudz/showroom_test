package test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

public class Main {

	/** YouTube用（資産管理しない）プロパティファイル */
	private static String YOUTUBE_PROPFILE = "youtube_noupload.properties";

	/** HTTPトランスポート */
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	/** JSONファクトリ */
	private static final JsonFactory JSON_FACTORY = new GsonFactory();

	/** HTTPリクエストの初期化クラス */
	private static final HttpRequestInitializer HTTP_REQUEST_INITIALIZER = new HttpRequestInitializer() {
		public void initialize(HttpRequest request) throws IOException {
		}
	};
	
	/** Youtubeオブジェクト */
	private static YouTube youtube;
	
	/** YouTubeのURL */
	private static final String YOUTUBE_URL = "http://www.youtube.com/watch?v=";

	/** 返される動画の最大数 (50 = ページあたりの上限) */
	private static final long NUMBER_OF_VIDEOS_RETURNED = 50;

	/** 検索ワード */
	private static final String SEARCH_KEYWORDS = "SHOWROOM";

	/**
	 * main method
	 *
	 * @param args command line args.
	 */
	public static void main(String[] args) {
		try {
			/* 事前準備 */
			// YouTube用（資産管理しない）プロパティファイル読み込み
			Properties properties = loadProperties(YOUTUBE_PROPFILE);
			
			// 初期化
			youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, HTTP_REQUEST_INITIALIZER)
					.setApplicationName("youtube-test").build();
			List<String> part = new ArrayList<>(Arrays.asList("id", "snippet"));
			YouTube.Search.List search = youtube.search().list(part);
			HttpHeaders headers = new HttpHeaders().setContentType("application/json");

			// 取得情報の設定
	        search.setRequestHeaders(headers);
			search.setKey(properties.getProperty("youtube.apikey"));
			search.setType(new ArrayList<>(Arrays.asList("video")));
			search.setFields("items(id/videoId,snippet/title),nextPageToken");
			

			// 検索条件
			// キーワード
			search.setQ(SEARCH_KEYWORDS);
			// ソート
			search.setOrder("date");
			// 1リクエストにおける取得件数
			search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
			
			/* 検索 */
			// 検索実行
			SearchListResponse searchResponse = null;
			List<SearchResult> searchResultList = new ArrayList<>();
			do {
				// 次ページ取得設定
				if (searchResponse != null && searchResponse.getNextPageToken()!= null) {
					search.setPageToken(searchResponse.getNextPageToken());
				}	
				
				searchResponse = search.execute();
				searchResultList.addAll(searchResponse.getItems());
				
				// 100件取得できたら処理終了
				if (searchResultList.size() >= 100) {
					if (searchResultList.size() == 100) {
						break;
					}
					// 101件以上取得した場合は切り捨てる
					searchResultList.subList(100, searchResultList.size() - 1);
					break;
				}
			} while (searchResponse.getNextPageToken() != null);

			/* 出力 */
			// 結果出力
			output(searchResultList);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * プロパティファイルを読み込みます。
	 * 
	 * @return 読み込んだプロパティファイルオブジェクト
	 */
	private static Properties loadProperties(String propName) {
		Properties properties = new Properties();
		try {
			InputStream in = Main.class.getResourceAsStream(propName);
			properties.load(in);

		} catch (IOException e) {
			System.err.println(
					"There was an error reading " + YOUTUBE_PROPFILE + ": " + e.getCause() + " : " + e.getMessage());
			System.exit(1);
		}
		return properties;
	}

	/**
	 * 検索結果をフォーマットして出力します。
	 * 
	 * @param searchResultList
	 */
	private static void output(List<SearchResult> searchResultList) {
		List<String> list = new ArrayList<>();

		// ヘッダ部
		list.add(SEARCH_KEYWORDS.concat("の検索結果"));

		// データ部
		int no = 1;
		for (SearchResult result : searchResultList) {
			StringBuilder sb = new StringBuilder();
			sb.append("No.").append(no).append("：");
			sb.append(YOUTUBE_URL).append(result.getId().getVideoId());
			list.add(sb.toString());

			no++;
		}

		// 出力
		for (String result : list) {
			System.out.println(result);
		}
	}
}
