package com.example.android.books;

import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

final class QueryUtils {

	/**
	 * Tag for the log messages
	 */
	private static final String LOG_TAG = QueryUtils.class.getSimpleName();

	/**
	 * Create a private constructor because no one should ever create a {@link QueryUtils} object.
	 */
	private QueryUtils() {
	}

	/**
	 * Query the Google Books via the API and return a list of {@link String} objects.
	 *
	 * @param requestUrl a {@link String} as an url
	 * @return the book titles of all the fetched books
	 */
	static List<Book> fetchBooks(String requestUrl) {
		// Create valid url object from the requestURL
		URL url = createUrl(requestUrl);

		// Initialize empty String object to hold the parsed JSON response
		String jsonResponse = "";

		// Perform HTTP request to the above created valid URL
		try {
			jsonResponse = makeHttpRequest(url);
		} catch (IOException e) {
			Log.e(LOG_TAG, "Problem making the HTTP request for the search criteria");
		}

		// Extract information from the JSON response for each book
		// Return list of books
		return QueryUtils.extractFeatures(jsonResponse);
	}

	/**
	 * Returns new URL object from the given string URL.
	 */
	private static URL createUrl(String stringUrl) {
		// Initialize an empty {@link URL} object to hold the parsed URL from the stringUrl
		URL url = null;

		// Parse valid URL from param stringUrl
		// Handle Malformed urls
		try {
			url = new URL(stringUrl);
		} catch (MalformedURLException e) {
			Log.e(LOG_TAG, "Problem building the url!");
		}

		// Return valid url
		return url;
	}

	/**
	 * Return a list of {@link String} objects that has been built up from
	 * parsing the given JSON response.
	 */
	private static List<Book> extractFeatures(String booksJSON) {
		// Exit early if no data was returned from the HTTP request
		if (TextUtils.isEmpty(booksJSON)) {
			return null;
		}

		// Initialize list of strings to hold the extracted books
		List<Book> allBooks = new ArrayList<>();

		// Traverse the raw JSON response parameter and mine for relevant information
		try {
			// Create JSON object from response
			JSONObject rawJSONResponse = new JSONObject(booksJSON);

			// Extract the array that holds the books
			JSONArray books = rawJSONResponse.getJSONArray("items");
			for (int i = 0; i < books.length(); i++) {
				// Get the current book
				JSONObject book = books.getJSONObject(i);
				// Get the current book's volume information
				JSONObject volume = book.getJSONObject("volumeInfo");
				// Get the book's title from the volume information
				String bookTitle = volume.getString("title");

				// Extract information on authors of the book
				// Initialize empty string to hold authors of the book
				String authors = "";
				// Check whether the JSON results contain information on authors of the book
				if (volume.has("authors")) {
					// JSON does have author information
					// Extract the array that holds the data
					JSONArray jsonAuthors = volume.getJSONArray("authors");
					// Find and store the number of authors present in the authors array
					int numberOfAuthors = jsonAuthors.length();
					// Traverse the json array and add authors to the newly initialized array
					for (int j = 0; j < numberOfAuthors; j++) {
						authors += jsonAuthors.getString(j) + "\n";
					}
				}

				// Make book from the extracted information
				if (authors.length() > 0) {
					// Add the book to the list
					allBooks.add(new Book(bookTitle, authors));
				} else {
					// There is no information on the author of the book
					// Add the book with only its title information
					allBooks.add(new Book(bookTitle));
				}
			}

		} catch (JSONException e) {
			// If an error is thrown when executing any of the above statements in the "try" block,
			// catch the exception here, so the app doesn't crash. Print a log message
			// with the message from the exception.
			Log.e(LOG_TAG, "Problem parsing the google books JSON results", e);
		}

		// Return the successfully parsed book titles as a {@link List} object
		return allBooks;
	}

	/**
	 * Make an HTTP request to the given URL and return a String as the response.
	 */
	private static String makeHttpRequest(URL url) throws IOException {
		// Initialize variable to hold the parsed json response
		String jsonResponse = "";

		// Return early if url is null
		if (url == null) {
			return jsonResponse;
		}

		// Initialize HTTP connection object
		HttpURLConnection urlConnection = null;

		// Initialize {@link InputStream} to hold response from request
		InputStream inputStream = null;

		try {
			// Establish connection to the url
			urlConnection = (HttpURLConnection) url.openConnection();

			// Set request type
			urlConnection.setRequestMethod("GET");

			// Set read and connection timeout in milliseconds
			// Basically, setting how long to wait on the request
			urlConnection.setReadTimeout(10000);
			urlConnection.setConnectTimeout(15000);

			// Establish connection to the url
			urlConnection.connect();

			// Check for successful connection
			if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
				// Connection successfully established
				inputStream = urlConnection.getInputStream();
				jsonResponse = readFromStream(inputStream);
			} else {
				Log.e(LOG_TAG, "Error while connecting. Error Code: " + urlConnection.getResponseCode());
			}
		} catch (IOException e) {
			e.getMessage();
			Log.e(LOG_TAG, "Problem encountered while retrieving book results");
		} finally {
			if (urlConnection != null) {
				// Disconnect the connection after successfully making the HTTP request
				urlConnection.disconnect();
			}
			if (inputStream != null) {
				// Close the stream after successfully parsing the request
				// This may throw an IOException which is why it is explicitly mentioned in the
				// method signature
				inputStream.close();
			}
		}

		// Return JSON as a {@link String}
		return jsonResponse;
	}

	/**
	 * Convert the {@link InputStream} into a String which contains the
	 * whole JSON response from the server.
	 */
	private static String readFromStream(InputStream inputStream) throws IOException {
		StringBuilder output = new StringBuilder();
		if (inputStream != null) {
			// Decode the bits
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));

			// Buffer the decoded characters
			BufferedReader reader = new BufferedReader(inputStreamReader);

			// Store a line of characters from the {@link BufferedReader}
			String line = reader.readLine();

			// If not end of buffered input stream, read next line and add to output
			while (line != null) {
				output.append(line);
				line = reader.readLine();
			}
		}

		// Convert the mutable characters sequence from the builder into a string and return
		return output.toString();
	}
}
