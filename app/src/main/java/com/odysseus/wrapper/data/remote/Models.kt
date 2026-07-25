package com.odysseus.wrapper.data.remote

import com.google.gson.annotations.SerializedName

// ═══════════════════════════════════════════════════════════════════════════════
// AUTH
// ═══════════════════════════════════════════════════════════════════════════════

data class LoginRequest(val username: String, val password: String, val remember: Boolean = true)
data class LoginResponse(val ok: Boolean, val username: String? = null, val requires_totp: Boolean = false)
data class AuthStatusResponse(val authenticated: Boolean, val username: String? = null, val is_admin: Boolean = false)
data class ChangePasswordRequest(val current_password: String, val new_password: String)

// ═══════════════════════════════════════════════════════════════════════════════
// SESSIONS / CHAT  — /api/sessions returns [...] directly
// ═══════════════════════════════════════════════════════════════════════════════

data class SessionItem(
    val id: String,
    val name: String = "",
    val model: String = "",
    val endpoint_url: String = "",
    val rag: Boolean = false,
    val archived: Boolean = false,
    val folder: String? = null,
    val total_tokens: Int = 0,
    val is_important: Boolean = false,
    val created_at: String = "",
    val updated_at: String = "",
    val last_message_at: String? = null,
    val has_documents: Boolean = false,
    val has_images: Boolean = false,
    val mode: String = "agent",
    val message_count: Int = 0
)

// /api/history/{id} returns {"history": [...]}
data class HistoryResponse(val history: List<MessageItem> = emptyList())

data class MessageItem(
    val role: String = "",        // "user" | "assistant"
    val content: String = "",
    val metadata: MessageMetadata? = null
)

data class MessageMetadata(
    val model: String? = null,
    val input_tokens: Int? = null,
    val output_tokens: Int? = null,
    val response_time: Double? = null
)

data class ChatRequest(
    val message: String,
    val session: String,
    val use_web: Boolean = false,
    val use_research: Boolean = false,
    val attachments: List<String> = emptyList()
)
data class ChatResponse(val response: String)

// /api/models returns {"hosts":[], "items":[...]}
data class ModelsResponse(
    val hosts: List<Any> = emptyList(),
    val items: List<ModelEndpointGroup> = emptyList()
)
data class ModelEndpointGroup(
    val host: String = "",
    val url: String = "",
    val models: List<String> = emptyList()
)

// /api/model-endpoints returns [...] directly
data class EndpointItem(
    val id: String = "",
    val name: String = "",
    val base_url: String = "",
    val has_key: Boolean = false,
    val is_enabled: Boolean = true,
    val models: List<String> = emptyList()
)

// ═══════════════════════════════════════════════════════════════════════════════
// NOTES — /api/notes returns {"notes": [...]}
// ═══════════════════════════════════════════════════════════════════════════════

data class NotesResponse(val notes: List<NoteItem> = emptyList())

data class NoteItem(
    val id: Int = 0,
    val title: String = "",
    val content: String? = null,
    val items: List<NoteCheckItem>? = null,
    val note_type: String = "note",
    val color: String? = null,
    val label: String? = null,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val due_date: String? = null,
    val repeat: String? = "none",
    val created_at: String = "",
    val updated_at: String = ""
)

data class NoteCheckItem(val text: String = "", val done: Boolean = false)

data class NoteCreateRequest(
    val title: String = "",
    val content: String? = null,
    val items: List<NoteCheckItem>? = null,
    val note_type: String = "note",
    val color: String? = null,
    val pinned: Boolean = false,
    val due_date: String? = null
)

data class NoteUpdateRequest(
    val title: String? = null,
    val content: String? = null,
    val items: List<NoteCheckItem>? = null,
    val note_type: String? = null,
    val color: String? = null,
    val pinned: Boolean? = null,
    val archived: Boolean? = null,
    val due_date: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// TASKS — /api/tasks returns {"tasks": [...]}
// ═══════════════════════════════════════════════════════════════════════════════

data class TasksResponse(val tasks: List<TaskItem> = emptyList())

data class TaskItem(
    val id: String = "",
    val name: String = "",
    val prompt: String? = null,
    val task_type: String = "llm",
    val action: String? = null,
    val schedule: String? = null,
    val scheduled_time: String? = null,
    val scheduled_day: Int? = null,
    val scheduled_date: String? = null,
    val cron_expression: String? = null,
    val trigger_type: String = "schedule",
    val trigger_event: String? = null,
    val output_target: String = "session",
    val model: String? = null,
    val endpoint_url: String? = null,
    val status: String = "active",
    val last_run: String? = null,
    val run_count: Int = 0,
    val notifications_enabled: Boolean = false
)

data class TaskCreateRequest(
    val name: String? = null,
    val prompt: String? = null,
    val task_type: String = "llm",
    val schedule: String? = null,
    val scheduled_time: String = "09:00",
    val scheduled_day: Int? = null,
    val scheduled_date: String? = null,
    val cron_expression: String? = null,
    val trigger_type: String = "schedule",
    val output_target: String = "session",
    val model: String? = null,
    val endpoint_url: String? = null,
    val notifications_enabled: Boolean? = null
)

data class TaskUpdateRequest(
    val name: String? = null,
    val prompt: String? = null,
    val schedule: String? = null,
    val scheduled_time: String? = null,
    val model: String? = null
)

data class TaskRun(
    val id: Int = 0,
    val task_id: String = "",
    val started_at: String = "",
    val finished_at: String? = null,
    val status: String = "",
    val result: String? = null,
    val error: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// CALENDAR — /api/calendar/calendars returns {"calendars": [...]}
//            /api/calendar/events requires ?start=&end= query params
// ═══════════════════════════════════════════════════════════════════════════════

data class CalendarsResponse(val calendars: List<CalendarItem> = emptyList())

data class CalendarItem(
    val href: String = "",
    val name: String = "",
    val color: String? = null,
    val source: String = "local"
)

data class CalendarEvent(
    val uid: String = "",
    val summary: String = "",
    val dtstart: String = "",
    val dtend: String? = null,
    val all_day: Boolean = false,
    val description: String = "",
    val location: String = "",
    val rrule: String? = null,
    val color: String? = null,
    val calendar_href: String? = null,
    val calendar_name: String? = null
)

data class EventCreateRequest(
    val summary: String,
    val dtstart: String,
    val dtend: String? = null,
    val all_day: Boolean = false,
    val description: String = "",
    val location: String = "",
    val calendar_href: String? = null,
    val rrule: String? = null,
    val color: String? = null
)

data class EventUpdateRequest(
    val summary: String? = null,
    val dtstart: String? = null,
    val dtend: String? = null,
    val all_day: Boolean? = null,
    val description: String? = null,
    val location: String? = null,
    val rrule: String? = null,
    val color: String? = null
)

// ═══════════════════════════════════════════════════════════════════════════════
// EMAIL
// ═══════════════════════════════════════════════════════════════════════════════

data class EmailItem(
    val uid: String = "",
    val subject: String = "",
    val sender: String = "",
    val sender_name: String = "",
    val date: String = "",
    val snippet: String = "",
    val is_read: Boolean = false,
    val is_flagged: Boolean = false,
    val has_attachments: Boolean = false,
    val folder: String = "INBOX",
    val account_id: Int = 0
)

data class EmailDetail(
    val uid: String = "",
    val subject: String = "",
    val sender: String = "",
    val sender_name: String = "",
    val to: List<String> = emptyList(),
    val cc: List<String> = emptyList(),
    val date: String = "",
    val body_html: String = "",
    val body_text: String = "",
    val is_read: Boolean = false,
    val is_flagged: Boolean = false,
    val attachments: List<EmailAttachment> = emptyList()
)

data class EmailAttachment(
    val index: Int = 0,
    val filename: String = "",
    val content_type: String = "",
    val size: Int = 0
)

data class SendEmailRequest(
    val account_id: Int,
    val to: String,
    val subject: String,
    val body: String,
    val cc: String = "",
    val reply_to_uid: String? = null
)

data class EmailFolder(val name: String = "", val full_name: String = "", val unread_count: Int = 0)

data class EmailAccountItem(
    val id: Int = 0,
    val label: String = "",
    val email: String = "",
    val imap_host: String = "",
    val smtp_host: String = "",
    val is_default: Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════════════
// DOCUMENTS — /api/documents/library returns {"documents": [...], "total": N}
// ═══════════════════════════════════════════════════════════════════════════════

data class DocumentsLibraryResponse(
    val documents: List<DocumentItem> = emptyList(),
    val total: Int = 0
)

data class DocumentItem(
    val id: String = "",
    val title: String = "",
    val content_type: String = "markdown",
    val created_at: String = "",
    val updated_at: String = "",
    val word_count: Int = 0,
    val is_archived: Boolean = false,
    val session_id: String? = null
)

data class DocumentDetail(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val content_type: String = "markdown",
    val created_at: String = "",
    val updated_at: String = ""
)

data class DocumentCreateRequest(val title: String, val content: String = "", val content_type: String = "markdown")
data class DocumentUpdateRequest(val title: String? = null, val content: String? = null)

// ═══════════════════════════════════════════════════════════════════════════════
// GALLERY
// ═══════════════════════════════════════════════════════════════════════════════

data class GalleryImage(
    val id: String = "",
    val filename: String = "",
    val url: String = "",
    val thumbnail_url: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val size: Int = 0,
    val created_at: String = "",
    val tags: List<String> = emptyList(),
    val ai_tags: List<String> = emptyList(),
    val is_favorite: Boolean = false,
    val album_ids: List<String> = emptyList()
)

data class GalleryAlbum(
    val id: String = "",
    val name: String = "",
    val cover_url: String? = null,
    val image_count: Int = 0,
    val created_at: String = ""
)

data class GalleryStats(
    val total_images: Int = 0,
    val total_size_bytes: Long = 0,
    val albums_count: Int = 0
)

// ═══════════════════════════════════════════════════════════════════════════════
// RESEARCH — /api/research/library returns {"research": [...]}
// ═══════════════════════════════════════════════════════════════════════════════

data class ResearchLibraryResponse(val research: List<ResearchSession> = emptyList())

data class ResearchSession(
    val id: String = "",
    val query: String = "",
    val status: String = "",
    val category: String = "",
    val source_count: Int = 0,
    val rounds: Int = 0,
    val started_at: Double = 0.0,
    val completed_at: Double? = null,
    val archived: Boolean = false,
    val thumbnail: String? = null
)

data class ResearchStartRequest(val query: String, val max_sources: Int = 10, val mode: String = "standard")

data class ResearchStatus(
    val session_id: String = "",
    val status: String = "",
    val progress: Int = 0,
    val message: String = "",
    val sources_found: Int = 0
)

data class ResearchReport(
    val session_id: String = "",
    val query: String = "",
    val report: String = "",
    val sources: List<ResearchSource> = emptyList(),
    val created_at: String = ""
)

data class ResearchSource(val url: String = "", val title: String = "", val snippet: String = "")

// ═══════════════════════════════════════════════════════════════════════════════
// MEMORY — /api/memory returns {"memory": [...]}
//          /api/skills returns {"skills": [...], "count": N}
// ═══════════════════════════════════════════════════════════════════════════════

data class MemoryListResponse(val memory: List<MemoryItem> = emptyList())

data class MemoryItem(
    val id: String = "",
    val text: String = "",
    val category: String = "fact",
    val source: String = "user",
    val timestamp: Long = 0,
    val session_id: String? = null,
    val pinned: Boolean = false
)

data class MemoryAddRequest(val text: String, val category: String = "fact", val source: String = "user")
data class MemoryUpdateRequest(val text: String, val category: String? = null)

data class SkillsResponse(val skills: List<SkillItem> = emptyList(), val count: Int = 0)

data class SkillItem(
    val id: String = "",
    val title: String = "",
    val problem: String = "",
    val solution: String = "",
    val tags: List<String> = emptyList(),
    val status: String = "approved",
    val created_at: String = ""
)

// ═══════════════════════════════════════════════════════════════════════════════
// GENERIC
// ═══════════════════════════════════════════════════════════════════════════════

data class GenericResponse(val ok: Boolean = true, val message: String? = null)
data class IdResponse(val id: String)
data class UploadResponse(val id: String, val name: String, val mime: String, val size: Int, val url: String? = null)
