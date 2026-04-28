package app.nouralroh.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface QuranApiService {

    /** All 114 chapters with metadata */
    @GET("chapters")
    suspend fun getChapters(
        @Query("language") language: String = "en"
    ): ChaptersResponse
    @GET("verses/by_page/{pageNumber}")
    suspend fun getVersesByPage(
        @Path("pageNumber") pageNumber: Int,
        @Query("language")                  language: String = "en",
        @Query("words")                     words: Boolean = true,
        @Query("per_page")                  perPage: Int = 300,
        @Query("word_fields")               wordFields: String = "text_uthmani,page_number,line_number,verse_key,char_type_name",
        @Query("word_translation_language") wordTranslationLanguage: String = "en",
        @Query("translations")              translations: Int = 131
    ): VersesResponse
}

object QuranApi {
    private const val BASE_URL = "https://api.qurancdn.com/api/v4/"

    val service: QuranApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuranApiService::class.java)
    }
}