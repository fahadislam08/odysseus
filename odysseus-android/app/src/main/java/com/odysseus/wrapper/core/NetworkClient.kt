package com.odysseus.wrapper.core

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// ── Cookie storage ────────────────────────────────────────────────────────────

class InMemoryCookieJar : CookieJar {
    private val store = mutableMapOf<String, MutableList<Cookie>>()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val list = store.getOrPut(url.host) { mutableListOf() }
        cookies.forEach { new -> list.removeAll { it.name == new.name }; list.add(new) }
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> = store[url.host] ?: emptyList()

    @Synchronized
    fun clear() = store.clear()

    @Synchronized
    fun hasSession(): Boolean = store.values.any { list ->
        list.any { it.name == "odysseus_session" }
    }
}

// ── Singleton network client ──────────────────────────────────────────────────

object NetworkClient {

    /**
     * Base URL of the Odysseus server.
     * Empty string = not configured yet (first-run state).
     */
    var baseUrl: String = ""
        set(value) {
            field = if (value.isNotEmpty() && !value.endsWith("/")) "$value/" else value
            invalidate()
        }

    /**
     * Optional bearer token (ody_… mobile token).
     * When set, every request gets  Authorization: Bearer <token>  injected.
     * Cookie-based sessions still work in parallel — the interceptor adds the
     * header only when a token is stored.
     */
    var bearerToken: String = ""
        set(value) { field = value; invalidate() }

    val cookieJar = InMemoryCookieJar()

    @Volatile private var _retrofit: Retrofit? = null
    @Volatile private var _okHttp: OkHttpClient? = null

    private fun invalidate() { _retrofit = null; _okHttp = null }

    private val okHttp: OkHttpClient
        get() = _okHttp ?: buildOkHttp().also { _okHttp = it }

    private fun buildOkHttp(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
        // Inject bearer token when available
        val token = bearerToken
        if (token.isNotEmpty()) {
            builder.addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(req)
            }
        }
        return builder.build()
    }

    val retrofit: Retrofit
        get() = _retrofit ?: Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build().also { _retrofit = it }

    inline fun <reified T> create(): T = retrofit.create(T::class.java)

    fun rawOkHttp(): OkHttpClient = okHttp
    fun clearSession() = cookieJar.clear()
    fun isLoggedIn() = cookieJar.hasSession()
    fun isConfigured() = baseUrl.isNotEmpty()
}
