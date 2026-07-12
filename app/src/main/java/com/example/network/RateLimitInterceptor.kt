package com.example.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class RateLimitInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response = chain.proceed(request)
        
        var tryCount = 0
        val maxLimit = 3
        var baseDelayMs = 2000L // Start with a 2-second delay

        while (response.code == 429 && tryCount < maxLimit) {
            tryCount++
            Log.w("RateLimitInterceptor", "HTTP 429 detected on request: ${request.url}. Retry attempt $tryCount of $maxLimit after ${baseDelayMs}ms delay...")
            
            // Close the previous response body to avoid connection leaks
            response.close()
            
            try {
                Thread.sleep(baseDelayMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
            
            // Exponential backoff
            baseDelayMs *= 2
            
            response = chain.proceed(request)
        }
        
        return response
    }
}
