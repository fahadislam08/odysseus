package com.odysseus.wrapper.data.repository

import com.odysseus.wrapper.core.NetworkClient
import com.odysseus.wrapper.data.remote.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private suspend fun <T> safeCall(block: suspend () -> retrofit2.Response<T>): Result<T> = try {
    val r = block()
    if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
    else Result.failure(Exception("HTTP ${r.code()}: ${r.errorBody()?.string()?.take(200)}"))
} catch (e: Exception) { Result.failure(e) }

class AuthRepository {
    private val api get() = NetworkClient.create<AuthApiService>()
    suspend fun login(u: String, p: String) = safeCall { api.login(LoginRequest(u, p)) }
    suspend fun logout() = safeCall { api.logout() }
    suspend fun status() = safeCall { api.status() }
    suspend fun changePassword(cur: String, new: String) = safeCall { api.changePassword(ChangePasswordRequest(cur, new)) }
}

class SessionRepository {
    private val sApi get() = NetworkClient.create<SessionApiService>()
    private val cApi get() = NetworkClient.create<ChatApiService>()

    suspend fun list() = safeCall { sApi.list() }
    suspend fun archived() = safeCall { sApi.archived() }
    suspend fun create(name: String = "") = safeCall { sApi.create(name = name) }
    suspend fun delete(id: String) = safeCall { sApi.delete(id) }
    suspend fun rename(id: String, name: String) = safeCall { sApi.rename(id, mapOf("name" to name)) }
    suspend fun archive(id: String) = safeCall { sApi.archive(id) }
    suspend fun unarchive(id: String) = safeCall { sApi.unarchive(id) }

    // Unwrap {"history":[...]} → List<MessageItem>
    suspend fun history(id: String): Result<List<MessageItem>> =
        safeCall { sApi.history(id) }.map { it.history }

    // Unwrap {"hosts":[],"items":[...]} → flat list of model strings
    suspend fun modelNames(): Result<List<String>> =
        safeCall { cApi.models() }.map { resp ->
            resp.items.flatMap { it.models }
        }

    suspend fun endpoints() = safeCall { cApi.endpoints() }
    suspend fun defaultChat() = safeCall { cApi.defaultChat() }
}

class ChatRepository {
    private val api get() = NetworkClient.create<ChatApiService>()
    suspend fun send(message: String, sessionId: String, useWeb: Boolean = false) =
        safeCall { api.send(ChatRequest(message, sessionId, use_web = useWeb)) }
    suspend fun stop(sessionId: String) = safeCall { api.stop(sessionId) }
}

class NotesRepository {
    private val api get() = NetworkClient.create<NotesApiService>()
    // Unwrap {"notes":[...]} → List<NoteItem>
    suspend fun list(type: String? = null, archived: Boolean = false): Result<List<NoteItem>> =
        safeCall { api.list(type, archived) }.map { it.notes }
    suspend fun create(req: NoteCreateRequest) = safeCall { api.create(req) }
    suspend fun get(id: Int) = safeCall { api.get(id) }
    suspend fun update(id: Int, req: NoteUpdateRequest) = safeCall { api.update(id, req) }
    suspend fun delete(id: Int) = safeCall { api.delete(id) }
    suspend fun pin(id: Int) = safeCall { api.pin(id) }
    suspend fun archive(id: Int) = safeCall { api.archive(id) }
    suspend fun toggleItem(id: Int, index: Int) = safeCall { api.toggleItem(id, index) }
}

class TasksRepository {
    private val api get() = NetworkClient.create<TasksApiService>()
    // Unwrap {"tasks":[...]} → List<TaskItem>
    suspend fun list(): Result<List<TaskItem>> =
        safeCall { api.list() }.map { it.tasks }
    suspend fun create(req: TaskCreateRequest) = safeCall { api.create(req) }
    suspend fun get(id: String) = safeCall { api.get(id) }
    suspend fun update(id: String, req: TaskUpdateRequest) = safeCall { api.update(id, req) }
    suspend fun delete(id: String) = safeCall { api.delete(id) }
    suspend fun run(id: String) = safeCall { api.run(id) }
    suspend fun pause(id: String) = safeCall { api.pause(id) }
    suspend fun resume(id: String) = safeCall { api.resume(id) }
    suspend fun stop(id: String) = safeCall { api.stop(id) }
    suspend fun runs(id: String) = safeCall { api.runs(id) }
    suspend fun recentRuns(): Result<List<TaskRun>> = try {
        val r = api.recentRuns()
        if (r.isSuccessful && r.body() != null) Result.success(r.body()!!)
        else Result.success(emptyList()) // recent runs may return empty
    } catch (e: Exception) { Result.success(emptyList()) }
}

class CalendarRepository {
    private val api get() = NetworkClient.create<CalendarApiService>()
    // Unwrap {"calendars":[...]} → List<CalendarItem>
    suspend fun calendars(): Result<List<CalendarItem>> =
        safeCall { api.calendars() }.map { it.calendars }
    // Events REQUIRE start/end — pass current month range
    suspend fun events(from: String? = null, to: String? = null): Result<List<CalendarEvent>> {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val start = from ?: LocalDate.now().withDayOfMonth(1).atStartOfDay().format(fmt)
        val end   = to   ?: LocalDate.now().plusMonths(2).withDayOfMonth(1).atStartOfDay().format(fmt)
        return safeCall { api.events(start, end) }
    }
    suspend fun createEvent(req: EventCreateRequest) = safeCall { api.createEvent(req) }
    suspend fun updateEvent(uid: String, req: EventUpdateRequest) = safeCall { api.updateEvent(uid, req) }
    suspend fun deleteEvent(uid: String) = safeCall { api.deleteEvent(uid) }
    suspend fun sync() = safeCall { api.sync() }
    suspend fun quickParse(text: String) = safeCall { api.quickParse(mapOf("text" to text)) }
}

class EmailRepository {
    private val api get() = NetworkClient.create<EmailApiService>()
    suspend fun list(folder: String = "INBOX", accountId: Int? = null, offset: Int = 0) = safeCall { api.list(folder, accountId, offset) }
    suspend fun read(uid: String, accountId: Int? = null) = safeCall { api.read(uid, accountId) }
    suspend fun send(req: SendEmailRequest) = safeCall { api.send(req) }
    suspend fun folders(accountId: Int? = null) = safeCall { api.folders(accountId) }
    suspend fun accounts() = safeCall { api.accounts() }
    suspend fun markRead(uid: String) = safeCall { api.markRead(uid) }
    suspend fun markUnread(uid: String) = safeCall { api.markUnread(uid) }
    suspend fun flag(uid: String) = safeCall { api.flag(uid) }
    suspend fun archive(uid: String) = safeCall { api.archiveEmail(uid) }
    suspend fun delete(uid: String) = safeCall { api.delete(uid) }
    suspend fun search(q: String, accountId: Int? = null) = safeCall { api.search(q, accountId) }
    suspend fun aiReply(uid: String, instruction: String = "") = safeCall { api.aiReply(mapOf("uid" to uid, "instruction" to instruction)) }
    suspend fun move(uid: String, folder: String) = safeCall { api.move(uid, mapOf("folder" to folder)) }
}

class DocumentsRepository {
    private val api get() = NetworkClient.create<DocumentsApiService>()
    // Unwrap {"documents":[...],"total":N} → List<DocumentItem>
    suspend fun library(q: String? = null): Result<List<DocumentItem>> =
        safeCall { api.library(q) }.map { it.documents }
    suspend fun create(req: DocumentCreateRequest) = safeCall { api.create(req) }
    suspend fun get(id: String) = safeCall { api.get(id) }
    suspend fun update(id: String, req: DocumentUpdateRequest) = safeCall { api.update(id, req) }
    suspend fun delete(id: String) = safeCall { api.delete(id) }
    suspend fun archive(id: String) = safeCall { api.archive(id) }
}

class GalleryRepository {
    private val api get() = NetworkClient.create<GalleryApiService>()
    suspend fun library(q: String? = null, offset: Int = 0, albumId: String? = null) = safeCall { api.library(q, offset, 40, albumId) }
    suspend fun upload(file: File): Result<GalleryImage> {
        val body = file.asRequestBody("image/*".toMediaType())
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        return safeCall { api.upload(part) }
    }
    suspend fun delete(id: String) = safeCall { api.delete(id) }
    suspend fun favorite(id: String) = safeCall { api.favorite(id) }
    suspend fun aiTag(id: String) = safeCall { api.aiTag(id) }
    suspend fun rename(id: String, name: String) = safeCall { api.rename(id, mapOf("name" to name)) }
    suspend fun albums() = safeCall { api.albums() }
    suspend fun createAlbum(name: String) = safeCall { api.createAlbum(mapOf("name" to name)) }
    suspend fun deleteAlbum(id: String) = safeCall { api.deleteAlbum(id) }
    suspend fun addToAlbum(albumId: String, ids: List<String>) = safeCall { api.addToAlbum(albumId, mapOf("image_ids" to ids)) }
    suspend fun stats() = safeCall { api.stats() }
    suspend fun aiTagBatch(ids: List<String>) = safeCall { api.aiTagBatch(mapOf("image_ids" to ids)) }
}

class ResearchRepository {
    private val api get() = NetworkClient.create<ResearchApiService>()
    // Unwrap {"research":[...]} → List<ResearchSession>
    suspend fun library(): Result<List<ResearchSession>> =
        safeCall { api.library() }.map { it.research }
    suspend fun start(query: String, maxSources: Int = 10) = safeCall { api.start(ResearchStartRequest(query, maxSources)) }
    suspend fun status(id: String) = safeCall { api.status(id) }
    suspend fun cancel(id: String) = safeCall { api.cancel(id) }
    suspend fun report(id: String) = safeCall { api.report(id) }
    suspend fun archive(id: String) = safeCall { api.archive(id) }
    suspend fun delete(id: String) = safeCall { api.delete(id) }
    suspend fun active() = safeCall { api.active() }
}

class MemoryRepository {
    private val mApi get() = NetworkClient.create<MemoryApiService>()
    private val sApi get() = NetworkClient.create<SkillsApiService>()
    // Unwrap {"memory":[...]} → List<MemoryItem>
    suspend fun listMemories(category: String? = null): Result<List<MemoryItem>> =
        safeCall { mApi.list(category) }.map { it.memory }
    suspend fun addMemory(text: String, category: String = "fact") = safeCall { mApi.add(MemoryAddRequest(text, category)) }
    suspend fun updateMemory(id: String, text: String, category: String? = null) = safeCall { mApi.update(id, MemoryUpdateRequest(text, category)) }
    suspend fun deleteMemory(id: String) = safeCall { mApi.delete(id) }
    suspend fun pinMemory(id: String) = safeCall { mApi.pin(id) }
    // Unwrap {"skills":[...],"count":N} → List<SkillItem>
    suspend fun listSkills(): Result<List<SkillItem>> =
        safeCall { sApi.list() }.map { it.skills }
    suspend fun createSkill(title: String, problem: String, solution: String, tags: String = "") =
        safeCall { sApi.create(mapOf("title" to title, "problem" to problem, "solution" to solution, "tags" to tags)) }
    suspend fun deleteSkill(id: String) = safeCall { sApi.delete(id) }
}
