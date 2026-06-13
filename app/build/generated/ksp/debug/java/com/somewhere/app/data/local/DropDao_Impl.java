package com.somewhere.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.somewhere.app.data.model.Drop;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DropDao_Impl implements DropDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Drop> __insertionAdapterOfDrop;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public DropDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDrop = new EntityInsertionAdapter<Drop>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `drops` (`id`,`text`,`imagePath`,`latitude`,`longitude`,`timestamp`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Drop entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getText());
        statement.bindString(3, entity.getImagePath());
        statement.bindDouble(4, entity.getLatitude());
        statement.bindDouble(5, entity.getLongitude());
        statement.bindLong(6, entity.getTimestamp());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM drops WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM drops";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Drop drop, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDrop.insert(drop);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Drop>> getAll() {
    final String _sql = "SELECT * FROM drops ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"drops"}, new Callable<List<Drop>>() {
      @Override
      @NonNull
      public List<Drop> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<Drop> _result = new ArrayList<Drop>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Drop _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new Drop(_tmpId,_tmpText,_tmpImagePath,_tmpLatitude,_tmpLongitude,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllOnce(final Continuation<? super List<Drop>> $completion) {
    final String _sql = "SELECT * FROM drops";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Drop>>() {
      @Override
      @NonNull
      public List<Drop> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<Drop> _result = new ArrayList<Drop>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Drop _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new Drop(_tmpId,_tmpText,_tmpImagePath,_tmpLatitude,_tmpLongitude,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getInBoundingBox(final double lat, final double lon, final double delta,
      final Continuation<? super List<Drop>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM drops \n"
            + "        WHERE latitude BETWEEN ? - ? AND ? + ? \n"
            + "        AND longitude BETWEEN ? - ? AND ? + ?\n"
            + "        ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 8);
    int _argIndex = 1;
    _statement.bindDouble(_argIndex, lat);
    _argIndex = 2;
    _statement.bindDouble(_argIndex, delta);
    _argIndex = 3;
    _statement.bindDouble(_argIndex, lat);
    _argIndex = 4;
    _statement.bindDouble(_argIndex, delta);
    _argIndex = 5;
    _statement.bindDouble(_argIndex, lon);
    _argIndex = 6;
    _statement.bindDouble(_argIndex, delta);
    _argIndex = 7;
    _statement.bindDouble(_argIndex, lon);
    _argIndex = 8;
    _statement.bindDouble(_argIndex, delta);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Drop>>() {
      @Override
      @NonNull
      public List<Drop> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfImagePath = CursorUtil.getColumnIndexOrThrow(_cursor, "imagePath");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final List<Drop> _result = new ArrayList<Drop>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Drop _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            final String _tmpImagePath;
            _tmpImagePath = _cursor.getString(_cursorIndexOfImagePath);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new Drop(_tmpId,_tmpText,_tmpImagePath,_tmpLatitude,_tmpLongitude,_tmpTimestamp);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
