package com.mobilerepair.shop.data.db.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.mobilerepair.shop.data.model.SparePartPurchase;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
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
public final class SparePartPurchaseDao_Impl implements SparePartPurchaseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SparePartPurchase> __insertionAdapterOfSparePartPurchase;

  private final EntityDeletionOrUpdateAdapter<SparePartPurchase> __deletionAdapterOfSparePartPurchase;

  private final EntityDeletionOrUpdateAdapter<SparePartPurchase> __updateAdapterOfSparePartPurchase;

  public SparePartPurchaseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSparePartPurchase = new EntityInsertionAdapter<SparePartPurchase>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `spare_part_purchases` (`id`,`repairEntryId`,`partName`,`partPhotoPath`,`purchasePrice`,`supplierId`,`supplierName`,`quantity`,`purchaseDate`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SparePartPurchase entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getRepairEntryId());
        statement.bindString(3, entity.getPartName());
        statement.bindString(4, entity.getPartPhotoPath());
        statement.bindDouble(5, entity.getPurchasePrice());
        statement.bindLong(6, entity.getSupplierId());
        statement.bindString(7, entity.getSupplierName());
        statement.bindLong(8, entity.getQuantity());
        statement.bindLong(9, entity.getPurchaseDate());
        statement.bindLong(10, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfSparePartPurchase = new EntityDeletionOrUpdateAdapter<SparePartPurchase>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `spare_part_purchases` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SparePartPurchase entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSparePartPurchase = new EntityDeletionOrUpdateAdapter<SparePartPurchase>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `spare_part_purchases` SET `id` = ?,`repairEntryId` = ?,`partName` = ?,`partPhotoPath` = ?,`purchasePrice` = ?,`supplierId` = ?,`supplierName` = ?,`quantity` = ?,`purchaseDate` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SparePartPurchase entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getRepairEntryId());
        statement.bindString(3, entity.getPartName());
        statement.bindString(4, entity.getPartPhotoPath());
        statement.bindDouble(5, entity.getPurchasePrice());
        statement.bindLong(6, entity.getSupplierId());
        statement.bindString(7, entity.getSupplierName());
        statement.bindLong(8, entity.getQuantity());
        statement.bindLong(9, entity.getPurchaseDate());
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final SparePartPurchase purchase,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSparePartPurchase.insertAndReturnId(purchase);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final SparePartPurchase purchase,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSparePartPurchase.handle(purchase);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final SparePartPurchase purchase,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSparePartPurchase.handle(purchase);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SparePartPurchase>> getPurchasesByRepairId(final long repairId) {
    final String _sql = "SELECT * FROM spare_part_purchases WHERE repairEntryId = ? ORDER BY purchaseDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, repairId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spare_part_purchases"}, new Callable<List<SparePartPurchase>>() {
      @Override
      @NonNull
      public List<SparePartPurchase> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRepairEntryId = CursorUtil.getColumnIndexOrThrow(_cursor, "repairEntryId");
          final int _cursorIndexOfPartName = CursorUtil.getColumnIndexOrThrow(_cursor, "partName");
          final int _cursorIndexOfPartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "partPhotoPath");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSupplierName = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierName");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SparePartPurchase> _result = new ArrayList<SparePartPurchase>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SparePartPurchase _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpRepairEntryId;
            _tmpRepairEntryId = _cursor.getLong(_cursorIndexOfRepairEntryId);
            final String _tmpPartName;
            _tmpPartName = _cursor.getString(_cursorIndexOfPartName);
            final String _tmpPartPhotoPath;
            _tmpPartPhotoPath = _cursor.getString(_cursorIndexOfPartPhotoPath);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final String _tmpSupplierName;
            _tmpSupplierName = _cursor.getString(_cursorIndexOfSupplierName);
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final long _tmpPurchaseDate;
            _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SparePartPurchase(_tmpId,_tmpRepairEntryId,_tmpPartName,_tmpPartPhotoPath,_tmpPurchasePrice,_tmpSupplierId,_tmpSupplierName,_tmpQuantity,_tmpPurchaseDate,_tmpCreatedAt);
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
  public Flow<List<SparePartPurchase>> getAllPurchases() {
    final String _sql = "SELECT * FROM spare_part_purchases ORDER BY purchaseDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spare_part_purchases"}, new Callable<List<SparePartPurchase>>() {
      @Override
      @NonNull
      public List<SparePartPurchase> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRepairEntryId = CursorUtil.getColumnIndexOrThrow(_cursor, "repairEntryId");
          final int _cursorIndexOfPartName = CursorUtil.getColumnIndexOrThrow(_cursor, "partName");
          final int _cursorIndexOfPartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "partPhotoPath");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSupplierName = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierName");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SparePartPurchase> _result = new ArrayList<SparePartPurchase>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SparePartPurchase _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpRepairEntryId;
            _tmpRepairEntryId = _cursor.getLong(_cursorIndexOfRepairEntryId);
            final String _tmpPartName;
            _tmpPartName = _cursor.getString(_cursorIndexOfPartName);
            final String _tmpPartPhotoPath;
            _tmpPartPhotoPath = _cursor.getString(_cursorIndexOfPartPhotoPath);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final String _tmpSupplierName;
            _tmpSupplierName = _cursor.getString(_cursorIndexOfSupplierName);
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final long _tmpPurchaseDate;
            _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SparePartPurchase(_tmpId,_tmpRepairEntryId,_tmpPartName,_tmpPartPhotoPath,_tmpPurchasePrice,_tmpSupplierId,_tmpSupplierName,_tmpQuantity,_tmpPurchaseDate,_tmpCreatedAt);
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
  public Flow<List<SparePartPurchase>> getPurchasesBySupplier(final long supplierId) {
    final String _sql = "SELECT * FROM spare_part_purchases WHERE supplierId = ? ORDER BY purchaseDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, supplierId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spare_part_purchases"}, new Callable<List<SparePartPurchase>>() {
      @Override
      @NonNull
      public List<SparePartPurchase> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRepairEntryId = CursorUtil.getColumnIndexOrThrow(_cursor, "repairEntryId");
          final int _cursorIndexOfPartName = CursorUtil.getColumnIndexOrThrow(_cursor, "partName");
          final int _cursorIndexOfPartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "partPhotoPath");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSupplierName = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierName");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SparePartPurchase> _result = new ArrayList<SparePartPurchase>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SparePartPurchase _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpRepairEntryId;
            _tmpRepairEntryId = _cursor.getLong(_cursorIndexOfRepairEntryId);
            final String _tmpPartName;
            _tmpPartName = _cursor.getString(_cursorIndexOfPartName);
            final String _tmpPartPhotoPath;
            _tmpPartPhotoPath = _cursor.getString(_cursorIndexOfPartPhotoPath);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final String _tmpSupplierName;
            _tmpSupplierName = _cursor.getString(_cursorIndexOfSupplierName);
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final long _tmpPurchaseDate;
            _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SparePartPurchase(_tmpId,_tmpRepairEntryId,_tmpPartName,_tmpPartPhotoPath,_tmpPurchasePrice,_tmpSupplierId,_tmpSupplierName,_tmpQuantity,_tmpPurchaseDate,_tmpCreatedAt);
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
  public Flow<List<SparePartPurchase>> getPurchasesByDateRange(final long startDate,
      final long endDate) {
    final String _sql = "SELECT * FROM spare_part_purchases WHERE purchaseDate BETWEEN ? AND ? ORDER BY purchaseDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spare_part_purchases"}, new Callable<List<SparePartPurchase>>() {
      @Override
      @NonNull
      public List<SparePartPurchase> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfRepairEntryId = CursorUtil.getColumnIndexOrThrow(_cursor, "repairEntryId");
          final int _cursorIndexOfPartName = CursorUtil.getColumnIndexOrThrow(_cursor, "partName");
          final int _cursorIndexOfPartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "partPhotoPath");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSupplierName = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierName");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfPurchaseDate = CursorUtil.getColumnIndexOrThrow(_cursor, "purchaseDate");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SparePartPurchase> _result = new ArrayList<SparePartPurchase>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SparePartPurchase _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpRepairEntryId;
            _tmpRepairEntryId = _cursor.getLong(_cursorIndexOfRepairEntryId);
            final String _tmpPartName;
            _tmpPartName = _cursor.getString(_cursorIndexOfPartName);
            final String _tmpPartPhotoPath;
            _tmpPartPhotoPath = _cursor.getString(_cursorIndexOfPartPhotoPath);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final String _tmpSupplierName;
            _tmpSupplierName = _cursor.getString(_cursorIndexOfSupplierName);
            final int _tmpQuantity;
            _tmpQuantity = _cursor.getInt(_cursorIndexOfQuantity);
            final long _tmpPurchaseDate;
            _tmpPurchaseDate = _cursor.getLong(_cursorIndexOfPurchaseDate);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SparePartPurchase(_tmpId,_tmpRepairEntryId,_tmpPartName,_tmpPartPhotoPath,_tmpPurchasePrice,_tmpSupplierId,_tmpSupplierName,_tmpQuantity,_tmpPurchaseDate,_tmpCreatedAt);
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
  public Flow<Double> getTotalPurchaseInRange(final long startDate, final long endDate) {
    final String _sql = "SELECT SUM(purchasePrice * quantity) FROM spare_part_purchases WHERE purchaseDate BETWEEN ? AND ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spare_part_purchases"}, new Callable<Double>() {
      @Override
      @Nullable
      public Double call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Double _result;
          if (_cursor.moveToFirst()) {
            final Double _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getDouble(0);
            }
            _result = _tmp;
          } else {
            _result = null;
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
  public Flow<Integer> getTotalPurchasesCount() {
    final String _sql = "SELECT COUNT(*) FROM spare_part_purchases";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"spare_part_purchases"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
