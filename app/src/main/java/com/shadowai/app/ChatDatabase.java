package com.shadowai.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;

public final class ChatDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "shadowai.db";
    private static final int DB_VERSION = 4;

    public ChatDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE chats (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, pinned INTEGER NOT NULL DEFAULT 0, project_id INTEGER NOT NULL DEFAULT -1)");
        db.execSQL("CREATE TABLE messages (_id INTEGER PRIMARY KEY AUTOINCREMENT, chat_id INTEGER NOT NULL, role TEXT NOT NULL, content TEXT NOT NULL, created_at INTEGER NOT NULL, FOREIGN KEY(chat_id) REFERENCES chats(_id) ON DELETE CASCADE)");
        db.execSQL("CREATE TABLE projects (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', context TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX messages_chat_idx ON messages(chat_id, _id)");
        db.execSQL("PRAGMA foreign_keys=ON");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE chats ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0");
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE chats ADD COLUMN project_id INTEGER NOT NULL DEFAULT -1");
            db.execSQL("CREATE TABLE projects (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, created_at INTEGER NOT NULL)");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE projects ADD COLUMN description TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE projects ADD COLUMN context TEXT NOT NULL DEFAULT ''");
        }
    }

    public long createChat(String title) {
        long now = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("title", title == null || title.trim().isEmpty() ? "Neuer Chat" : title.trim());
        v.put("created_at", now);
        v.put("updated_at", now);
        return getWritableDatabase().insertOrThrow("chats", null, v);
    }

    public long latestChatId() {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id FROM chats ORDER BY updated_at DESC LIMIT 1", null)) {
            return c.moveToFirst() ? c.getLong(0) : -1;
        }
    }

    public void ensureChat(long id) {
        if (id < 0) createChat("Neuer Chat");
    }

    public void addMessage(long chatId, String role, String content) {
        long now = System.currentTimeMillis();
        ContentValues v = new ContentValues();
        v.put("chat_id", chatId);
        v.put("role", role);
        v.put("content", content);
        v.put("created_at", now);
        getWritableDatabase().insertOrThrow("messages", null, v);

        ContentValues u = new ContentValues();
        u.put("updated_at", now);
        if ("user".equals(role)) {
            u.put("title", content.length() > 42 ? content.substring(0, 42) + "…" : content);
        }
        getWritableDatabase().update("chats", u, "_id=?", new String[]{String.valueOf(chatId)});
    }

    public void deleteMessagesFrom(long chatId,long messageId) {
        getWritableDatabase().delete("messages","chat_id=? AND _id>=?",new String[]{String.valueOf(chatId),String.valueOf(messageId)});
    }

    public JSONArray messages(long chatId) {
        JSONArray out = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,role,content,created_at FROM messages WHERE chat_id=? ORDER BY _id",
                new String[]{String.valueOf(chatId)})) {
            while (c.moveToNext()) {
                JSONObject m = new JSONObject();
                m.put("id", c.getLong(0));
                m.put("role", c.getString(1));
                m.put("content", c.getString(2));
                m.put("created_at", c.getLong(3));
                out.put(m);
            }
        } catch (Exception ignored) { }
        return out;
    }

    public JSONArray getChats() {
        JSONArray out = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,title,created_at,updated_at,pinned,project_id FROM chats ORDER BY pinned DESC, updated_at DESC",
                null)) {
            while (c.moveToNext()) {
                JSONObject chat = new JSONObject();
                chat.put("id", c.getLong(0));
                chat.put("title", c.getString(1));
                chat.put("created_at", c.getLong(2));
                chat.put("updated_at", c.getLong(3));
                chat.put("pinned", c.getInt(4) == 1);
                chat.put("project_id", c.getLong(5));
                out.put(chat);
            }
        } catch (Exception ignored) { }
        return out;
    }

    public boolean renameChat(long chatId, String title) {
        if (title == null || title.trim().isEmpty()) return false;
        ContentValues v = new ContentValues();
        v.put("title", title.trim());
        v.put("updated_at", System.currentTimeMillis());
        return getWritableDatabase().update(
                "chats", v, "_id=?", new String[]{String.valueOf(chatId)}) > 0;
    }

    public boolean setPinned(long chatId, boolean pinned) {
        ContentValues v = new ContentValues();
        v.put("pinned", pinned ? 1 : 0);
        return getWritableDatabase().update(
                "chats", v, "_id=?", new String[]{String.valueOf(chatId)}) > 0;
    }

    public boolean updateMessage(long chatId, long messageId, String content) {
        if (content == null || content.trim().isEmpty()) return false;

        ContentValues v = new ContentValues();
        v.put("content", content);

        return getWritableDatabase().update(
                "messages",
                v,
                "_id=? AND chat_id=? AND role='user'",
                new String[]{String.valueOf(messageId), String.valueOf(chatId)}
        ) > 0;
    }

    public JSONArray search(String query) {
        JSONArray out = new JSONArray();
        if (query == null || query.trim().isEmpty()) return out;

        String pattern = "%" + query.trim() + "%";

        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT DISTINCT c._id,c.title,c.pinned " +
                "FROM chats c LEFT JOIN messages m ON m.chat_id=c._id " +
                "WHERE c.title LIKE ? OR m.content LIKE ? " +
                "ORDER BY c.pinned DESC,c.updated_at DESC",
                new String[]{pattern, pattern})) {
            while (c.moveToNext()) {
                JSONObject item = new JSONObject();
                item.put("id", c.getLong(0));
                item.put("title", c.getString(1));
                item.put("pinned", c.getInt(2) == 1);
                out.put(item);
            }
        } catch (Exception ignored) { }
        return out;
    }

    public JSONArray exportAll() {
        JSONArray chats = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,title,created_at,pinned FROM chats ORDER BY _id", null)) {
            while (c.moveToNext()) {
                JSONObject chat = new JSONObject();
                chat.put("id", c.getLong(0));
                chat.put("title", c.getString(1));
                chat.put("created_at", c.getLong(2));
                chat.put("pinned", c.getInt(3) == 1);
                chat.put("messages", messages(c.getLong(0)));
                chats.put(chat);
            }
        } catch (Exception ignored) { }
        return chats;
    }


    public JSONArray exportChat(long chatId) {
        JSONArray result = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,title,created_at,pinned FROM chats WHERE _id=?",
                new String[]{String.valueOf(chatId)})) {
            if (c.moveToFirst()) {
                JSONObject chat = new JSONObject();
                chat.put("id", c.getLong(0));
                chat.put("title", c.getString(1));
                chat.put("created_at", c.getLong(2));
                chat.put("pinned", c.getInt(3) == 1);
                chat.put("messages", messages(chatId));
                result.put(chat);
            }
        } catch (Exception ignored) { }
        return result;
    }

    public long importChat(String title, JSONArray messages) {

        long id = createChat(title);
        for (int i = 0; i < messages.length(); i++) {
            JSONObject m = messages.optJSONObject(i);
            if (m == null) continue;

            String role = m.optString("role", "");
            if (("user".equals(role) || "assistant".equals(role) || "system".equals(role))
                    && m.has("content")) {
                addMessage(id, role, m.optString("content"));
            }
        }
        return id;
    }

    public void clearChat(long id) {
        getWritableDatabase().delete("messages", "chat_id=?",
                new String[]{String.valueOf(id)});
    }

    public long createProject(String name) {
        if (name == null || name.trim().isEmpty()) return -1;
        ContentValues v = new ContentValues();
        v.put("name", name.trim());
        v.put("created_at", System.currentTimeMillis());
        return getWritableDatabase().insert("projects", null, v);
    }

    public JSONArray getProjects() {
        JSONArray out = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT _id,name,description,context FROM projects ORDER BY name COLLATE NOCASE", null)) {
            while (c.moveToNext()) {
                JSONObject p = new JSONObject();
                p.put("id", c.getLong(0));
                p.put("name", c.getString(1));
                p.put("description", c.getString(2));
                p.put("context", c.getString(3));
                out.put(p);
            }
        } catch (Exception ignored) { }
        return out;
    }

    public boolean assignChatToProject(long chatId, long projectId) {
        ContentValues v = new ContentValues();
        v.put("project_id", projectId);
        return getWritableDatabase().update("chats", v, "_id=?", new String[]{String.valueOf(chatId)}) > 0;
    }

    public JSONArray getProjectChats(long projectId) {
        JSONArray out = new JSONArray();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT _id,title,pinned FROM chats WHERE project_id=? ORDER BY pinned DESC,updated_at DESC", new String[]{String.valueOf(projectId)})) {
            while (c.moveToNext()) {
                JSONObject p = new JSONObject();
                p.put("id", c.getLong(0));
                p.put("title", c.getString(1));
                p.put("pinned", c.getInt(2) == 1);
                out.put(p);
            }
        } catch (Exception ignored) { }
        return out;
    }


    public boolean renameProject(long projectId,String name) {
        if(name==null || name.trim().isEmpty())return false;
        ContentValues values=new ContentValues();
        values.put("name",name.trim());
        return getWritableDatabase().update("projects",values,"_id=?",
            new String[]{String.valueOf(projectId)})>0;
    }

    public boolean deleteProject(long projectId) {

        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("project_id", -1);
        db.update("chats", v, "project_id=?", new String[]{String.valueOf(projectId)});
        return db.delete("projects", "_id=?", new String[]{String.valueOf(projectId)}) > 0;
    }


    public boolean updateProject(long projectId, String name, String description, String context) {
        if (name == null || name.trim().isEmpty()) return false;
        ContentValues v = new ContentValues();
        v.put("name", name.trim());
        v.put("description", description == null ? "" : description.trim());
        v.put("context", context == null ? "" : context.trim());
        return getWritableDatabase().update("projects", v, "_id=?",
                new String[]{String.valueOf(projectId)}) > 0;
    }

    public JSONObject getProject(long projectId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT _id,name,description,context FROM projects WHERE _id=?",
                new String[]{String.valueOf(projectId)})) {
            if (c.moveToFirst()) {
                JSONObject p = new JSONObject();
                p.put("id", c.getLong(0));
                p.put("name", c.getString(1));
                p.put("description", c.getString(2));
                p.put("context", c.getString(3));
                return p;
            }
        } catch (Exception ignored) { }
        return null;
    }

    public JSONObject getProjectForChat(long chatId) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT p._id,p.name,p.description,p.context FROM projects p " +
                "INNER JOIN chats ch ON ch.project_id=p._id WHERE ch._id=?",
                new String[]{String.valueOf(chatId)})) {
            if (c.moveToFirst()) {
                JSONObject p = new JSONObject();
                p.put("id", c.getLong(0));
                p.put("name", c.getString(1));
                p.put("description", c.getString(2));
                p.put("context", c.getString(3));
                return p;
            }
        } catch (Exception ignored) { }
        return null;
    }

    public boolean removeChatFromProject(long chatId) {
        ContentValues v = new ContentValues();
        v.put("project_id", -1);
        return getWritableDatabase().update("chats", v, "_id=?",
                new String[]{String.valueOf(chatId)}) > 0;
    }

    public void deleteChat(long id) {
        getWritableDatabase().delete("messages", "chat_id=?",
                new String[]{String.valueOf(id)});
        getWritableDatabase().delete("chats", "_id=?",
                new String[]{String.valueOf(id)});
    }
}
