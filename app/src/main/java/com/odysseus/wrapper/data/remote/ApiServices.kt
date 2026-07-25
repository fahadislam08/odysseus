package com.odysseus.wrapper.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): Response<LoginResponse>
    @POST("api/auth/logout")
    suspend fun logout(): Response<GenericResponse>
    @GET("api/auth/status")
    suspend fun status(): Response<AuthStatusResponse>
    @POST("api/auth/change-password")
    suspend fun changePassword(@Body req: ChangePasswordRequest): Response<GenericResponse>
}

// /api/sessions → List<SessionItem> directly ✓
// /api/history/{id} → {"history":[...]} ← must use HistoryResponse wrapper
interface SessionApiService {
    @GET("api/sessions")
    suspend fun list(): Response<List<SessionItem>>
    @FormUrlEncoded
    @POST("api/session")
    suspend fun create(
        @Field("name") name: String = "",
        @Field("endpoint_url") endpointUrl: String = "",
        @Field("model") model: String = "",
        @Field("skip_validation") skipValidation: String = "true"
    ): Response<SessionItem>
    @DELETE("api/session/{id}")
    suspend fun delete(@Path("id") id: String): Response<GenericResponse>
    @PATCH("api/session/{id}")
    suspend fun rename(@Path("id") id: String, @Body body: Map<String, String>): Response<GenericResponse>
    @POST("api/session/{id}/archive")
    suspend fun archive(@Path("id") id: String): Response<GenericResponse>
    @POST("api/session/{id}/unarchive")
    suspend fun unarchive(@Path("id") id: String): Response<GenericResponse>
    @GET("api/history/{id}")
    suspend fun history(@Path("id") id: String): Response<HistoryResponse>   // {"history":[...]}
    @GET("api/sessions/archived")
    suspend fun archived(): Response<List<SessionItem>>
}

interface ChatApiService {
    @POST("api/chat")
    suspend fun send(@Body req: ChatRequest): Response<ChatResponse>
    @POST("api/chat/stop/{id}")
    suspend fun stop(@Path("id") id: String): Response<GenericResponse>
    @GET("api/models")
    suspend fun models(): Response<ModelsResponse>          // {"hosts":[],"items":[...]}
    @GET("api/model-endpoints")
    suspend fun endpoints(): Response<List<EndpointItem>>   // [...] directly ✓
    @GET("api/default-chat")
    suspend fun defaultChat(): Response<Map<String, String>>
}

// /api/notes → {"notes":[...]}
interface NotesApiService {
    @GET("api/notes")
    suspend fun list(@Query("note_type") type: String? = null, @Query("archived") archived: Boolean = false): Response<NotesResponse>
    @POST("api/notes")
    suspend fun create(@Body req: NoteCreateRequest): Response<NoteItem>
    @GET("api/notes/{id}")
    suspend fun get(@Path("id") id: Int): Response<NoteItem>
    @PUT("api/notes/{id}")
    suspend fun update(@Path("id") id: Int, @Body req: NoteUpdateRequest): Response<NoteItem>
    @DELETE("api/notes/{id}")
    suspend fun delete(@Path("id") id: Int): Response<GenericResponse>
    @POST("api/notes/{id}/pin")
    suspend fun pin(@Path("id") id: Int): Response<GenericResponse>
    @POST("api/notes/{id}/archive")
    suspend fun archive(@Path("id") id: Int): Response<GenericResponse>
    @POST("api/notes/{id}/items/{index}/toggle")
    suspend fun toggleItem(@Path("id") id: Int, @Path("index") index: Int): Response<NoteItem>
}

// /api/tasks → {"tasks":[...]}
interface TasksApiService {
    @GET("api/tasks")
    suspend fun list(): Response<TasksResponse>
    @POST("api/tasks")
    suspend fun create(@Body req: TaskCreateRequest): Response<TaskItem>
    @GET("api/tasks/{id}")
    suspend fun get(@Path("id") id: String): Response<TaskItem>
    @PUT("api/tasks/{id}")
    suspend fun update(@Path("id") id: String, @Body req: TaskUpdateRequest): Response<TaskItem>
    @DELETE("api/tasks/{id}")
    suspend fun delete(@Path("id") id: String): Response<GenericResponse>
    @POST("api/tasks/{id}/run")
    suspend fun run(@Path("id") id: String): Response<GenericResponse>
    @POST("api/tasks/{id}/pause")
    suspend fun pause(@Path("id") id: String): Response<GenericResponse>
    @POST("api/tasks/{id}/resume")
    suspend fun resume(@Path("id") id: String): Response<GenericResponse>
    @POST("api/tasks/{id}/stop")
    suspend fun stop(@Path("id") id: String): Response<GenericResponse>
    @GET("api/tasks/{id}/runs")
    suspend fun runs(@Path("id") id: String): Response<List<TaskRun>>
    @GET("api/tasks/runs/recent")
    suspend fun recentRuns(): Response<List<TaskRun>>
}

// /api/calendar/calendars → {"calendars":[...]}
// /api/calendar/events → REQUIRES ?start=ISO&end=ISO query params
interface CalendarApiService {
    @GET("api/calendar/calendars")
    suspend fun calendars(): Response<CalendarsResponse>
    @GET("api/calendar/events")
    suspend fun events(@Query("start") start: String, @Query("end") end: String): Response<List<CalendarEvent>>
    @POST("api/calendar/events")
    suspend fun createEvent(@Body req: EventCreateRequest): Response<CalendarEvent>
    @PUT("api/calendar/events/{uid}")
    suspend fun updateEvent(@Path("uid") uid: String, @Body req: EventUpdateRequest): Response<CalendarEvent>
    @DELETE("api/calendar/events/{uid}")
    suspend fun deleteEvent(@Path("uid") uid: String): Response<GenericResponse>
    @POST("api/calendar/sync")
    suspend fun sync(): Response<GenericResponse>
    @POST("api/calendar/quick-parse")
    suspend fun quickParse(@Body body: Map<String, String>): Response<CalendarEvent>
}

interface EmailApiService {
    @GET("api/email/list")
    suspend fun list(@Query("folder") folder: String = "INBOX", @Query("account_id") accountId: Int? = null, @Query("offset") offset: Int = 0, @Query("limit") limit: Int = 40): Response<List<EmailItem>>
    @GET("api/email/read/{uid}")
    suspend fun read(@Path("uid") uid: String, @Query("account_id") accountId: Int? = null): Response<EmailDetail>
    @POST("api/email/emails/send")
    suspend fun send(@Body req: SendEmailRequest): Response<GenericResponse>
    @GET("api/email/folders")
    suspend fun folders(@Query("account_id") accountId: Int? = null): Response<List<EmailFolder>>
    @GET("api/email/config/accounts")
    suspend fun accounts(): Response<List<EmailAccountItem>>
    @POST("api/email/mark-read/{uid}")
    suspend fun markRead(@Path("uid") uid: String): Response<GenericResponse>
    @POST("api/email/mark-unread/{uid}")
    suspend fun markUnread(@Path("uid") uid: String): Response<GenericResponse>
    @POST("api/email/flag/{uid}")
    suspend fun flag(@Path("uid") uid: String): Response<GenericResponse>
    @POST("api/email/archive/{uid}")
    suspend fun archiveEmail(@Path("uid") uid: String): Response<GenericResponse>
    @DELETE("api/email/delete/{uid}")
    suspend fun delete(@Path("uid") uid: String): Response<GenericResponse>
    @GET("api/email/search")
    suspend fun search(@Query("q") q: String, @Query("account_id") accountId: Int? = null): Response<List<EmailItem>>
    @POST("api/email/ai-reply")
    suspend fun aiReply(@Body body: Map<String, String>): Response<Map<String, String>>
    @POST("api/email/move/{uid}")
    suspend fun move(@Path("uid") uid: String, @Body body: Map<String, String>): Response<GenericResponse>
}

// /api/documents/library → {"documents":[...],"total":N}
interface DocumentsApiService {
    @GET("api/documents/library")
    suspend fun library(@Query("q") q: String? = null): Response<DocumentsLibraryResponse>
    @POST("api/document")
    suspend fun create(@Body req: DocumentCreateRequest): Response<DocumentDetail>
    @GET("api/document/{id}")
    suspend fun get(@Path("id") id: String): Response<DocumentDetail>
    @PUT("api/document/{id}")
    suspend fun update(@Path("id") id: String, @Body req: DocumentUpdateRequest): Response<DocumentDetail>
    @DELETE("api/document/{id}")
    suspend fun delete(@Path("id") id: String): Response<GenericResponse>
    @POST("api/document/{id}/archive")
    suspend fun archive(@Path("id") id: String): Response<GenericResponse>
}

interface GalleryApiService {
    @GET("api/gallery/library")
    suspend fun library(@Query("q") q: String? = null, @Query("offset") offset: Int = 0, @Query("limit") limit: Int = 40, @Query("album_id") albumId: String? = null): Response<List<GalleryImage>>
    @Multipart
    @POST("api/gallery/upload")
    suspend fun upload(@Part file: MultipartBody.Part): Response<GalleryImage>
    @DELETE("api/gallery/{id}")
    suspend fun delete(@Path("id") id: String): Response<GenericResponse>
    @POST("api/gallery/{id}/favorite")
    suspend fun favorite(@Path("id") id: String): Response<GenericResponse>
    @POST("api/gallery/{id}/ai-tag")
    suspend fun aiTag(@Path("id") id: String): Response<GalleryImage>
    @POST("api/gallery/{id}/rename")
    suspend fun rename(@Path("id") id: String, @Body body: Map<String, String>): Response<GenericResponse>
    @GET("api/gallery/albums")
    suspend fun albums(): Response<List<GalleryAlbum>>
    @POST("api/gallery/albums")
    suspend fun createAlbum(@Body body: Map<String, String>): Response<GalleryAlbum>
    @DELETE("api/gallery/albums/{id}")
    suspend fun deleteAlbum(@Path("id") id: String): Response<GenericResponse>
    @POST("api/gallery/albums/{id}/add")
    suspend fun addToAlbum(@Path("id") albumId: String, @Body body: Map<String, List<String>>): Response<GenericResponse>
    @GET("api/gallery/stats")
    suspend fun stats(): Response<GalleryStats>
    @POST("api/gallery/ai-tag-batch")
    suspend fun aiTagBatch(@Body body: Map<String, List<String>>): Response<GenericResponse>
}

// /api/research/library → {"research":[...]}
interface ResearchApiService {
    @GET("api/research/library")
    suspend fun library(): Response<ResearchLibraryResponse>
    @POST("api/research/start")
    suspend fun start(@Body req: ResearchStartRequest): Response<ResearchSession>
    @GET("api/research/status/{id}")
    suspend fun status(@Path("id") id: String): Response<ResearchStatus>
    @POST("api/research/cancel/{id}")
    suspend fun cancel(@Path("id") id: String): Response<GenericResponse>
    @GET("api/research/report/{id}")
    suspend fun report(@Path("id") id: String): Response<ResearchReport>
    @POST("api/research/{id}/archive")
    suspend fun archive(@Path("id") id: String): Response<GenericResponse>
    @DELETE("api/research/{id}")
    suspend fun delete(@Path("id") id: String): Response<GenericResponse>
    @GET("api/research/active")
    suspend fun active(): Response<List<ResearchSession>>
}

// /api/memory → {"memory":[...]}
// /api/skills → {"skills":[...],"count":N}
interface MemoryApiService {
    @GET("api/memory")
    suspend fun list(@Query("category") category: String? = null): Response<MemoryListResponse>
    @POST("api/memory/add")
    suspend fun add(@Body req: MemoryAddRequest): Response<MemoryItem>
    @PUT("api/memory/{id}")
    suspend fun update(@Path("id") id: String, @Body req: MemoryUpdateRequest): Response<MemoryItem>
    @DELETE("api/memory/{id}")
    suspend fun delete(@Path("id") id: String): Response<GenericResponse>
    @POST("api/memory/{id}/pin")
    suspend fun pin(@Path("id") id: String): Response<GenericResponse>
}

interface SkillsApiService {
    @GET("api/skills")
    suspend fun list(): Response<SkillsResponse>
    @POST("api/skills")
    suspend fun create(@Body body: Map<String, String>): Response<SkillItem>
    @DELETE("api/skills/{id}")
    suspend fun delete(@Path("id") id: String): Response<GenericResponse>
}

interface UploadsApiService {
    @Multipart
    @POST("api/upload")
    suspend fun upload(@Part file: MultipartBody.Part): Response<UploadResponse>
}
