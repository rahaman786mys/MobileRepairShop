package com.app.muzzutech.data.db.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.app.muzzutech.data.model.Sale;
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
public final class SaleDao_Impl implements SaleDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Sale> __insertionAdapterOfSale;

  public SaleDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSale = new EntityInsertionAdapter<Sale>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `sales` (`id`,`itemName`,`supplierId`,`purchasePrice`,`salePrice`,`saleDate`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Sale entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getItemName());
        statement.bindString(3, entity.getSupplierId());
        statement.bindDouble(4, entity.getPurchasePrice());
        statement.bindDouble(5, entity.getSalePrice());
        statement.bindLong(6, entity.getSaleDate());
      }
    };
  }

  @Override
  public Object insert(final Sale sale, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSale.insert(sale);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Sale>> getAllSales() {
    final String _sql = "SELECT * FROM sales ORDER BY saleDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sales"}, new Callable<List<Sale>>() {
      @Override
      @NonNull
      public List<Sale> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfItemName = CursorUtil.getColumnIndexOrThrow(_cursor, "itemName");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "purchasePrice");
          final int _cursorIndexOfSalePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "salePrice");
          final int _cursorIndexOfSaleDate = CursorUtil.getColumnIndexOrThrow(_cursor, "saleDate");
          final List<Sale> _result = new ArrayList<Sale>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Sale _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpItemName;
            _tmpItemName = _cursor.getString(_cursorIndexOfItemName);
            final String _tmpSupplierId;
            _tmpSupplierId = _cursor.getString(_cursorIndexOfSupplierId);
            final double _tmpPurchasePrice;
            _tmpPurchasePrice = _cursor.getDouble(_cursorIndexOfPurchasePrice);
            final double _tmpSalePrice;
            _tmpSalePrice = _cursor.getDouble(_cursorIndexOfSalePrice);
            final long _tmpSaleDate;
            _tmpSaleDate = _cursor.getLong(_cursorIndexOfSaleDate);
            _item = new Sale(_tmpId,_tmpItemName,_tmpSupplierId,_tmpPurchasePrice,_tmpSalePrice,_tmpSaleDate);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
