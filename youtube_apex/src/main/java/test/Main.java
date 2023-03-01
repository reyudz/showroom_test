package test;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.time.DateUtils;

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

	/** まるひプロパティファイル */
	private static String YOUTUBE_PROPERTIES = "youtube_secret.properties";

	/** 検索用プロパティファイル */
	private static String SEARCH_PROPERTIES = "search.properties";

	/** Youtubeオブジェクト */
	private static YouTube youtube;

	/** HTTPトランスポート */
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

	/** JSONファクトリ */
	private static final JsonFactory JSON_FACTORY = new GsonFactory();

	/** HTTPリクエストの初期化クラス */
	private static final HttpRequestInitializer HTTP_REQUEST_INITIALIZER = new HttpRequestInitializer() {
		public void initialize(HttpRequest request) throws IOException {
		}
	};

	/**
	 * メイン処理です。
	 *
	 * @param args command line args.
	 */
	public static void main(String[] args) {
		try {
			/* 事前準備 */
			// プロパティファイル読み込み
			Properties secretProp = loadProperties(YOUTUBE_PROPERTIES);
			Properties searchProp = loadProperties(SEARCH_PROPERTIES);

			// 初期化
			youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, HTTP_REQUEST_INITIALIZER)
					.setApplicationName("test-app").build();
			List<String> part = new ArrayList<>(Arrays.asList("id", "snippet"));
			YouTube.Search.List search = youtube.search().list(part);
			HttpHeaders headers = new HttpHeaders().setContentType("application/json");

			// 取得情報の設定
			search.setRequestHeaders(headers);
			search.setKey(secretProp.getProperty("YOUTUBE_APIKEY"));
			search.setType(new ArrayList<>(Arrays.asList("video")));
			search.setFields("regionCode,items(id/videoId,snippet/title),nextPageToken");

			// 検索条件
			// キーワード
			search.setQ(searchProp.getProperty("SEARCH_KEYWORDS"));
			// アップロード日
			search.setPublishedAfter(
					getPublishedAfter(Integer.parseInt(searchProp.getProperty("DAY_OF_PUBLISHED_AFTER"))));
			// ソート
			search.setOrder(searchProp.getProperty("ORDER"));
			// 地域
			search.setRegionCode(searchProp.getProperty("REGION_CODE"));
			// 1リクエストにおける取得件数
			search.setMaxResults(Long.parseLong(searchProp.getProperty("NUMBER_OF_VIDEOS_RETURNED")));			

			/* 検索 */
			// 検索実行
			SearchListResponse searchResponse = search.execute();
			List<SearchResult> searchResultList = searchResponse.getItems();

			/* 出力 */
			// 結果出力
			output(searchProp.getProperty("SEARCH_KEYWORDS"), searchResultList);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 検索結果をフォーマットして出力します。
	 * 
	 * @param header           ヘッダ
	 * @param searchResultList 検索結果リスト
	 */
	private static void output(String header, List<SearchResult> searchResultList) {
		List<String> list = new ArrayList<>();

		// ヘッダ部
		list.add(header.concat("の人気動画ランキングTop10"));

		// データ部
		int no = 1;
		for (SearchResult result : searchResultList) {
			StringBuilder sb = new StringBuilder();

			sb.append("第").append(no).append("位").append(System.lineSeparator());
			sb.append("タイトル：").append(result.getSnippet().getTitle()).append(System.lineSeparator());
			sb.append("URL：").append(loadProperties(SEARCH_PROPERTIES).getProperty("WATCH_URL"))
					.append(result.getId().getVideoId());
			list.add(sb.toString());

			no++;
		}

		// 出力
		for (String result : list) {
			System.out.println(result);
		}
	}

	/**
	 * アップロード日のFromを取得します。
	 * 
	 * @param sub 今日を基準とした○日前
	 * @return 今日から○日を引いた日付
	 */
	private static String getPublishedAfter(int sub) {
		Date today = DateUtils.truncate(new Date(), Calendar.DAY_OF_MONTH);
		Date afterThreeDays = DateUtils.truncate(DateUtils.addDays(today, sub), Calendar.DAY_OF_MONTH);
	
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(afterThreeDays);
	}

	/**
	 * プロパティファイルを読み込みます。
	 * 
	 * @param propName プロパティファイル名
	 * @return 読み込んだプロパティファイルオブジェクト
	 */
	private static Properties loadProperties(String propName) {
		Properties properties = new Properties();
		try {
			InputStream in = Main.class.getResourceAsStream(propName);
			properties.load(in);

		} catch (IOException e) {
			System.err.println("There was an error reading " + propName + ": " + e.getCause() + " : " + e.getMessage());
			System.exit(1);
		}
		return properties;
	}
}
