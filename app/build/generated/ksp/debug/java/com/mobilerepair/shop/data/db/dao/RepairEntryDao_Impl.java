package com.mobilerepair.shop.data.db.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
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
import com.mobilerepair.shop.data.model.RepairEntry;
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
public final class RepairEntryDao_Impl implements RepairEntryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<RepairEntry> __insertionAdapterOfRepairEntry;

  private final EntityDeletionOrUpdateAdapter<RepairEntry> __deletionAdapterOfRepairEntry;

  private final EntityDeletionOrUpdateAdapter<RepairEntry> __updateAdapterOfRepairEntry;

  public RepairEntryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfRepairEntry = new EntityInsertionAdapter<RepairEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `repair_entries` (`id`,`deviceBrand`,`deviceModel`,`entryPhotoPath`,`customerName`,`customerMobile`,`customerCity`,`dealerName`,`dealerMobile`,`serviceManId`,`entryDate`,`faultDetected`,`faultDescription`,`additionalFaults`,`inspectionPhotoPath`,`inspectionDate`,`inspectionDone`,`chargeAmount`,`advanceAmount`,`quotationDate`,`quotationDone`,`sparePartPhotoPath`,`sparePartName`,`sparePartPurchasePrice`,`supplierId`,`sparePartDate`,`sparePartDone`,`workStatus`,`completionDate`,`workDone`,`finalAmount`,`paymentMode`,`onlineAmount`,`cashAmount`,`handoverDate`,`handoverDone`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RepairEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDeviceBrand());
        statement.bindString(3, entity.getDeviceModel());
        statement.bindString(4, entity.getEntryPhotoPath());
        statement.bindString(5, entity.getCustomerName());
        statement.bindString(6, entity.getCustomerMobile());
        statement.bindString(7, entity.getCustomerCity());
        statement.bindString(8, entity.getDealerName());
        statement.bindString(9, entity.getDealerMobile());
        statement.bindLong(10, entity.getServiceManId());
        statement.bindLong(11, entity.getEntryDate());
        statement.bindString(12, entity.getFaultDetected());
        statement.bindString(13, entity.getFaultDescription());
        statement.bindString(14, entity.getAdditionalFaults());
        statement.bindString(15, entity.getInspectionPhotoPath());
        statement.bindLong(16, entity.getInspectionDate());
        final int _tmp = entity.getInspectionDone() ? 1 : 0;
        statement.bindLong(17, _tmp);
        statement.bindDouble(18, entity.getChargeAmount());
        statement.bindDouble(19, entity.getAdvanceAmount());
        statement.bindLong(20, entity.getQuotationDate());
        final int _tmp_1 = entity.getQuotationDone() ? 1 : 0;
        statement.bindLong(21, _tmp_1);
        statement.bindString(22, entity.getSparePartPhotoPath());
        statement.bindString(23, entity.getSparePartName());
        statement.bindDouble(24, entity.getSparePartPurchasePrice());
        statement.bindLong(25, entity.getSupplierId());
        statement.bindLong(26, entity.getSparePartDate());
        final int _tmp_2 = entity.getSparePartDone() ? 1 : 0;
        statement.bindLong(27, _tmp_2);
        statement.bindString(28, entity.getWorkStatus());
        statement.bindLong(29, entity.getCompletionDate());
        final int _tmp_3 = entity.getWorkDone() ? 1 : 0;
        statement.bindLong(30, _tmp_3);
        statement.bindDouble(31, entity.getFinalAmount());
        statement.bindString(32, entity.getPaymentMode());
        statement.bindDouble(33, entity.getOnlineAmount());
        statement.bindDouble(34, entity.getCashAmount());
        statement.bindLong(35, entity.getHandoverDate());
        final int _tmp_4 = entity.getHandoverDone() ? 1 : 0;
        statement.bindLong(36, _tmp_4);
        statement.bindLong(37, entity.getCreatedAt());
        statement.bindLong(38, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfRepairEntry = new EntityDeletionOrUpdateAdapter<RepairEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `repair_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RepairEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfRepairEntry = new EntityDeletionOrUpdateAdapter<RepairEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `repair_entries` SET `id` = ?,`deviceBrand` = ?,`deviceModel` = ?,`entryPhotoPath` = ?,`customerName` = ?,`customerMobile` = ?,`customerCity` = ?,`dealerName` = ?,`dealerMobile` = ?,`serviceManId` = ?,`entryDate` = ?,`faultDetected` = ?,`faultDescription` = ?,`additionalFaults` = ?,`inspectionPhotoPath` = ?,`inspectionDate` = ?,`inspectionDone` = ?,`chargeAmount` = ?,`advanceAmount` = ?,`quotationDate` = ?,`quotationDone` = ?,`sparePartPhotoPath` = ?,`sparePartName` = ?,`sparePartPurchasePrice` = ?,`supplierId` = ?,`sparePartDate` = ?,`sparePartDone` = ?,`workStatus` = ?,`completionDate` = ?,`workDone` = ?,`finalAmount` = ?,`paymentMode` = ?,`onlineAmount` = ?,`cashAmount` = ?,`handoverDate` = ?,`handoverDone` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final RepairEntry entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getDeviceBrand());
        statement.bindString(3, entity.getDeviceModel());
        statement.bindString(4, entity.getEntryPhotoPath());
        statement.bindString(5, entity.getCustomerName());
        statement.bindString(6, entity.getCustomerMobile());
        statement.bindString(7, entity.getCustomerCity());
        statement.bindString(8, entity.getDealerName());
        statement.bindString(9, entity.getDealerMobile());
        statement.bindLong(10, entity.getServiceManId());
        statement.bindLong(11, entity.getEntryDate());
        statement.bindString(12, entity.getFaultDetected());
        statement.bindString(13, entity.getFaultDescription());
        statement.bindString(14, entity.getAdditionalFaults());
        statement.bindString(15, entity.getInspectionPhotoPath());
        statement.bindLong(16, entity.getInspectionDate());
        final int _tmp = entity.getInspectionDone() ? 1 : 0;
        statement.bindLong(17, _tmp);
        statement.bindDouble(18, entity.getChargeAmount());
        statement.bindDouble(19, entity.getAdvanceAmount());
        statement.bindLong(20, entity.getQuotationDate());
        final int _tmp_1 = entity.getQuotationDone() ? 1 : 0;
        statement.bindLong(21, _tmp_1);
        statement.bindString(22, entity.getSparePartPhotoPath());
        statement.bindString(23, entity.getSparePartName());
        statement.bindDouble(24, entity.getSparePartPurchasePrice());
        statement.bindLong(25, entity.getSupplierId());
        statement.bindLong(26, entity.getSparePartDate());
        final int _tmp_2 = entity.getSparePartDone() ? 1 : 0;
        statement.bindLong(27, _tmp_2);
        statement.bindString(28, entity.getWorkStatus());
        statement.bindLong(29, entity.getCompletionDate());
        final int _tmp_3 = entity.getWorkDone() ? 1 : 0;
        statement.bindLong(30, _tmp_3);
        statement.bindDouble(31, entity.getFinalAmount());
        statement.bindString(32, entity.getPaymentMode());
        statement.bindDouble(33, entity.getOnlineAmount());
        statement.bindDouble(34, entity.getCashAmount());
        statement.bindLong(35, entity.getHandoverDate());
        final int _tmp_4 = entity.getHandoverDone() ? 1 : 0;
        statement.bindLong(36, _tmp_4);
        statement.bindLong(37, entity.getCreatedAt());
        statement.bindLong(38, entity.getUpdatedAt());
        statement.bindLong(39, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final RepairEntry entry, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfRepairEntry.insertAndReturnId(entry);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final RepairEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfRepairEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final RepairEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfRepairEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<RepairEntry>> getAllEntries() {
    final String _sql = "SELECT * FROM repair_entries ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repair_entries"}, new Callable<List<RepairEntry>>() {
      @Override
      @NonNull
      public List<RepairEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceBrand");
          final int _cursorIndexOfDeviceModel = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceModel");
          final int _cursorIndexOfEntryPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "entryPhotoPath");
          final int _cursorIndexOfCustomerName = CursorUtil.getColumnIndexOrThrow(_cursor, "customerName");
          final int _cursorIndexOfCustomerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "customerMobile");
          final int _cursorIndexOfCustomerCity = CursorUtil.getColumnIndexOrThrow(_cursor, "customerCity");
          final int _cursorIndexOfDealerName = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerName");
          final int _cursorIndexOfDealerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerMobile");
          final int _cursorIndexOfServiceManId = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceManId");
          final int _cursorIndexOfEntryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "entryDate");
          final int _cursorIndexOfFaultDetected = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDetected");
          final int _cursorIndexOfFaultDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDescription");
          final int _cursorIndexOfAdditionalFaults = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalFaults");
          final int _cursorIndexOfInspectionPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionPhotoPath");
          final int _cursorIndexOfInspectionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDate");
          final int _cursorIndexOfInspectionDone = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDone");
          final int _cursorIndexOfChargeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "chargeAmount");
          final int _cursorIndexOfAdvanceAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "advanceAmount");
          final int _cursorIndexOfQuotationDate = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDate");
          final int _cursorIndexOfQuotationDone = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDone");
          final int _cursorIndexOfSparePartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPhotoPath");
          final int _cursorIndexOfSparePartName = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartName");
          final int _cursorIndexOfSparePartPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPurchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSparePartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDate");
          final int _cursorIndexOfSparePartDone = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDone");
          final int _cursorIndexOfWorkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "workStatus");
          final int _cursorIndexOfCompletionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "completionDate");
          final int _cursorIndexOfWorkDone = CursorUtil.getColumnIndexOrThrow(_cursor, "workDone");
          final int _cursorIndexOfFinalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "finalAmount");
          final int _cursorIndexOfPaymentMode = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentMode");
          final int _cursorIndexOfOnlineAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "onlineAmount");
          final int _cursorIndexOfCashAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "cashAmount");
          final int _cursorIndexOfHandoverDate = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDate");
          final int _cursorIndexOfHandoverDone = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<RepairEntry> _result = new ArrayList<RepairEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RepairEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDeviceBrand;
            _tmpDeviceBrand = _cursor.getString(_cursorIndexOfDeviceBrand);
            final String _tmpDeviceModel;
            _tmpDeviceModel = _cursor.getString(_cursorIndexOfDeviceModel);
            final String _tmpEntryPhotoPath;
            _tmpEntryPhotoPath = _cursor.getString(_cursorIndexOfEntryPhotoPath);
            final String _tmpCustomerName;
            _tmpCustomerName = _cursor.getString(_cursorIndexOfCustomerName);
            final String _tmpCustomerMobile;
            _tmpCustomerMobile = _cursor.getString(_cursorIndexOfCustomerMobile);
            final String _tmpCustomerCity;
            _tmpCustomerCity = _cursor.getString(_cursorIndexOfCustomerCity);
            final String _tmpDealerName;
            _tmpDealerName = _cursor.getString(_cursorIndexOfDealerName);
            final String _tmpDealerMobile;
            _tmpDealerMobile = _cursor.getString(_cursorIndexOfDealerMobile);
            final long _tmpServiceManId;
            _tmpServiceManId = _cursor.getLong(_cursorIndexOfServiceManId);
            final long _tmpEntryDate;
            _tmpEntryDate = _cursor.getLong(_cursorIndexOfEntryDate);
            final String _tmpFaultDetected;
            _tmpFaultDetected = _cursor.getString(_cursorIndexOfFaultDetected);
            final String _tmpFaultDescription;
            _tmpFaultDescription = _cursor.getString(_cursorIndexOfFaultDescription);
            final String _tmpAdditionalFaults;
            _tmpAdditionalFaults = _cursor.getString(_cursorIndexOfAdditionalFaults);
            final String _tmpInspectionPhotoPath;
            _tmpInspectionPhotoPath = _cursor.getString(_cursorIndexOfInspectionPhotoPath);
            final long _tmpInspectionDate;
            _tmpInspectionDate = _cursor.getLong(_cursorIndexOfInspectionDate);
            final boolean _tmpInspectionDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfInspectionDone);
            _tmpInspectionDone = _tmp != 0;
            final double _tmpChargeAmount;
            _tmpChargeAmount = _cursor.getDouble(_cursorIndexOfChargeAmount);
            final double _tmpAdvanceAmount;
            _tmpAdvanceAmount = _cursor.getDouble(_cursorIndexOfAdvanceAmount);
            final long _tmpQuotationDate;
            _tmpQuotationDate = _cursor.getLong(_cursorIndexOfQuotationDate);
            final boolean _tmpQuotationDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfQuotationDone);
            _tmpQuotationDone = _tmp_1 != 0;
            final String _tmpSparePartPhotoPath;
            _tmpSparePartPhotoPath = _cursor.getString(_cursorIndexOfSparePartPhotoPath);
            final String _tmpSparePartName;
            _tmpSparePartName = _cursor.getString(_cursorIndexOfSparePartName);
            final double _tmpSparePartPurchasePrice;
            _tmpSparePartPurchasePrice = _cursor.getDouble(_cursorIndexOfSparePartPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final long _tmpSparePartDate;
            _tmpSparePartDate = _cursor.getLong(_cursorIndexOfSparePartDate);
            final boolean _tmpSparePartDone;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSparePartDone);
            _tmpSparePartDone = _tmp_2 != 0;
            final String _tmpWorkStatus;
            _tmpWorkStatus = _cursor.getString(_cursorIndexOfWorkStatus);
            final long _tmpCompletionDate;
            _tmpCompletionDate = _cursor.getLong(_cursorIndexOfCompletionDate);
            final boolean _tmpWorkDone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWorkDone);
            _tmpWorkDone = _tmp_3 != 0;
            final double _tmpFinalAmount;
            _tmpFinalAmount = _cursor.getDouble(_cursorIndexOfFinalAmount);
            final String _tmpPaymentMode;
            _tmpPaymentMode = _cursor.getString(_cursorIndexOfPaymentMode);
            final double _tmpOnlineAmount;
            _tmpOnlineAmount = _cursor.getDouble(_cursorIndexOfOnlineAmount);
            final double _tmpCashAmount;
            _tmpCashAmount = _cursor.getDouble(_cursorIndexOfCashAmount);
            final long _tmpHandoverDate;
            _tmpHandoverDate = _cursor.getLong(_cursorIndexOfHandoverDate);
            final boolean _tmpHandoverDone;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfHandoverDone);
            _tmpHandoverDone = _tmp_4 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new RepairEntry(_tmpId,_tmpDeviceBrand,_tmpDeviceModel,_tmpEntryPhotoPath,_tmpCustomerName,_tmpCustomerMobile,_tmpCustomerCity,_tmpDealerName,_tmpDealerMobile,_tmpServiceManId,_tmpEntryDate,_tmpFaultDetected,_tmpFaultDescription,_tmpAdditionalFaults,_tmpInspectionPhotoPath,_tmpInspectionDate,_tmpInspectionDone,_tmpChargeAmount,_tmpAdvanceAmount,_tmpQuotationDate,_tmpQuotationDone,_tmpSparePartPhotoPath,_tmpSparePartName,_tmpSparePartPurchasePrice,_tmpSupplierId,_tmpSparePartDate,_tmpSparePartDone,_tmpWorkStatus,_tmpCompletionDate,_tmpWorkDone,_tmpFinalAmount,_tmpPaymentMode,_tmpOnlineAmount,_tmpCashAmount,_tmpHandoverDate,_tmpHandoverDone,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getEntryById(final long id, final Continuation<? super RepairEntry> $completion) {
    final String _sql = "SELECT * FROM repair_entries WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<RepairEntry>() {
      @Override
      @Nullable
      public RepairEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceBrand");
          final int _cursorIndexOfDeviceModel = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceModel");
          final int _cursorIndexOfEntryPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "entryPhotoPath");
          final int _cursorIndexOfCustomerName = CursorUtil.getColumnIndexOrThrow(_cursor, "customerName");
          final int _cursorIndexOfCustomerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "customerMobile");
          final int _cursorIndexOfCustomerCity = CursorUtil.getColumnIndexOrThrow(_cursor, "customerCity");
          final int _cursorIndexOfDealerName = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerName");
          final int _cursorIndexOfDealerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerMobile");
          final int _cursorIndexOfServiceManId = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceManId");
          final int _cursorIndexOfEntryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "entryDate");
          final int _cursorIndexOfFaultDetected = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDetected");
          final int _cursorIndexOfFaultDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDescription");
          final int _cursorIndexOfAdditionalFaults = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalFaults");
          final int _cursorIndexOfInspectionPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionPhotoPath");
          final int _cursorIndexOfInspectionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDate");
          final int _cursorIndexOfInspectionDone = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDone");
          final int _cursorIndexOfChargeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "chargeAmount");
          final int _cursorIndexOfAdvanceAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "advanceAmount");
          final int _cursorIndexOfQuotationDate = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDate");
          final int _cursorIndexOfQuotationDone = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDone");
          final int _cursorIndexOfSparePartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPhotoPath");
          final int _cursorIndexOfSparePartName = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartName");
          final int _cursorIndexOfSparePartPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPurchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSparePartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDate");
          final int _cursorIndexOfSparePartDone = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDone");
          final int _cursorIndexOfWorkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "workStatus");
          final int _cursorIndexOfCompletionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "completionDate");
          final int _cursorIndexOfWorkDone = CursorUtil.getColumnIndexOrThrow(_cursor, "workDone");
          final int _cursorIndexOfFinalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "finalAmount");
          final int _cursorIndexOfPaymentMode = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentMode");
          final int _cursorIndexOfOnlineAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "onlineAmount");
          final int _cursorIndexOfCashAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "cashAmount");
          final int _cursorIndexOfHandoverDate = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDate");
          final int _cursorIndexOfHandoverDone = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final RepairEntry _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDeviceBrand;
            _tmpDeviceBrand = _cursor.getString(_cursorIndexOfDeviceBrand);
            final String _tmpDeviceModel;
            _tmpDeviceModel = _cursor.getString(_cursorIndexOfDeviceModel);
            final String _tmpEntryPhotoPath;
            _tmpEntryPhotoPath = _cursor.getString(_cursorIndexOfEntryPhotoPath);
            final String _tmpCustomerName;
            _tmpCustomerName = _cursor.getString(_cursorIndexOfCustomerName);
            final String _tmpCustomerMobile;
            _tmpCustomerMobile = _cursor.getString(_cursorIndexOfCustomerMobile);
            final String _tmpCustomerCity;
            _tmpCustomerCity = _cursor.getString(_cursorIndexOfCustomerCity);
            final String _tmpDealerName;
            _tmpDealerName = _cursor.getString(_cursorIndexOfDealerName);
            final String _tmpDealerMobile;
            _tmpDealerMobile = _cursor.getString(_cursorIndexOfDealerMobile);
            final long _tmpServiceManId;
            _tmpServiceManId = _cursor.getLong(_cursorIndexOfServiceManId);
            final long _tmpEntryDate;
            _tmpEntryDate = _cursor.getLong(_cursorIndexOfEntryDate);
            final String _tmpFaultDetected;
            _tmpFaultDetected = _cursor.getString(_cursorIndexOfFaultDetected);
            final String _tmpFaultDescription;
            _tmpFaultDescription = _cursor.getString(_cursorIndexOfFaultDescription);
            final String _tmpAdditionalFaults;
            _tmpAdditionalFaults = _cursor.getString(_cursorIndexOfAdditionalFaults);
            final String _tmpInspectionPhotoPath;
            _tmpInspectionPhotoPath = _cursor.getString(_cursorIndexOfInspectionPhotoPath);
            final long _tmpInspectionDate;
            _tmpInspectionDate = _cursor.getLong(_cursorIndexOfInspectionDate);
            final boolean _tmpInspectionDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfInspectionDone);
            _tmpInspectionDone = _tmp != 0;
            final double _tmpChargeAmount;
            _tmpChargeAmount = _cursor.getDouble(_cursorIndexOfChargeAmount);
            final double _tmpAdvanceAmount;
            _tmpAdvanceAmount = _cursor.getDouble(_cursorIndexOfAdvanceAmount);
            final long _tmpQuotationDate;
            _tmpQuotationDate = _cursor.getLong(_cursorIndexOfQuotationDate);
            final boolean _tmpQuotationDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfQuotationDone);
            _tmpQuotationDone = _tmp_1 != 0;
            final String _tmpSparePartPhotoPath;
            _tmpSparePartPhotoPath = _cursor.getString(_cursorIndexOfSparePartPhotoPath);
            final String _tmpSparePartName;
            _tmpSparePartName = _cursor.getString(_cursorIndexOfSparePartName);
            final double _tmpSparePartPurchasePrice;
            _tmpSparePartPurchasePrice = _cursor.getDouble(_cursorIndexOfSparePartPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final long _tmpSparePartDate;
            _tmpSparePartDate = _cursor.getLong(_cursorIndexOfSparePartDate);
            final boolean _tmpSparePartDone;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSparePartDone);
            _tmpSparePartDone = _tmp_2 != 0;
            final String _tmpWorkStatus;
            _tmpWorkStatus = _cursor.getString(_cursorIndexOfWorkStatus);
            final long _tmpCompletionDate;
            _tmpCompletionDate = _cursor.getLong(_cursorIndexOfCompletionDate);
            final boolean _tmpWorkDone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWorkDone);
            _tmpWorkDone = _tmp_3 != 0;
            final double _tmpFinalAmount;
            _tmpFinalAmount = _cursor.getDouble(_cursorIndexOfFinalAmount);
            final String _tmpPaymentMode;
            _tmpPaymentMode = _cursor.getString(_cursorIndexOfPaymentMode);
            final double _tmpOnlineAmount;
            _tmpOnlineAmount = _cursor.getDouble(_cursorIndexOfOnlineAmount);
            final double _tmpCashAmount;
            _tmpCashAmount = _cursor.getDouble(_cursorIndexOfCashAmount);
            final long _tmpHandoverDate;
            _tmpHandoverDate = _cursor.getLong(_cursorIndexOfHandoverDate);
            final boolean _tmpHandoverDone;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfHandoverDone);
            _tmpHandoverDone = _tmp_4 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new RepairEntry(_tmpId,_tmpDeviceBrand,_tmpDeviceModel,_tmpEntryPhotoPath,_tmpCustomerName,_tmpCustomerMobile,_tmpCustomerCity,_tmpDealerName,_tmpDealerMobile,_tmpServiceManId,_tmpEntryDate,_tmpFaultDetected,_tmpFaultDescription,_tmpAdditionalFaults,_tmpInspectionPhotoPath,_tmpInspectionDate,_tmpInspectionDone,_tmpChargeAmount,_tmpAdvanceAmount,_tmpQuotationDate,_tmpQuotationDone,_tmpSparePartPhotoPath,_tmpSparePartName,_tmpSparePartPurchasePrice,_tmpSupplierId,_tmpSparePartDate,_tmpSparePartDone,_tmpWorkStatus,_tmpCompletionDate,_tmpWorkDone,_tmpFinalAmount,_tmpPaymentMode,_tmpOnlineAmount,_tmpCashAmount,_tmpHandoverDate,_tmpHandoverDone,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
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
  public Flow<RepairEntry> getEntryByIdFlow(final long id) {
    final String _sql = "SELECT * FROM repair_entries WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repair_entries"}, new Callable<RepairEntry>() {
      @Override
      @Nullable
      public RepairEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceBrand");
          final int _cursorIndexOfDeviceModel = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceModel");
          final int _cursorIndexOfEntryPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "entryPhotoPath");
          final int _cursorIndexOfCustomerName = CursorUtil.getColumnIndexOrThrow(_cursor, "customerName");
          final int _cursorIndexOfCustomerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "customerMobile");
          final int _cursorIndexOfCustomerCity = CursorUtil.getColumnIndexOrThrow(_cursor, "customerCity");
          final int _cursorIndexOfDealerName = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerName");
          final int _cursorIndexOfDealerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerMobile");
          final int _cursorIndexOfServiceManId = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceManId");
          final int _cursorIndexOfEntryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "entryDate");
          final int _cursorIndexOfFaultDetected = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDetected");
          final int _cursorIndexOfFaultDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDescription");
          final int _cursorIndexOfAdditionalFaults = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalFaults");
          final int _cursorIndexOfInspectionPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionPhotoPath");
          final int _cursorIndexOfInspectionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDate");
          final int _cursorIndexOfInspectionDone = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDone");
          final int _cursorIndexOfChargeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "chargeAmount");
          final int _cursorIndexOfAdvanceAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "advanceAmount");
          final int _cursorIndexOfQuotationDate = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDate");
          final int _cursorIndexOfQuotationDone = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDone");
          final int _cursorIndexOfSparePartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPhotoPath");
          final int _cursorIndexOfSparePartName = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartName");
          final int _cursorIndexOfSparePartPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPurchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSparePartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDate");
          final int _cursorIndexOfSparePartDone = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDone");
          final int _cursorIndexOfWorkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "workStatus");
          final int _cursorIndexOfCompletionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "completionDate");
          final int _cursorIndexOfWorkDone = CursorUtil.getColumnIndexOrThrow(_cursor, "workDone");
          final int _cursorIndexOfFinalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "finalAmount");
          final int _cursorIndexOfPaymentMode = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentMode");
          final int _cursorIndexOfOnlineAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "onlineAmount");
          final int _cursorIndexOfCashAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "cashAmount");
          final int _cursorIndexOfHandoverDate = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDate");
          final int _cursorIndexOfHandoverDone = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final RepairEntry _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDeviceBrand;
            _tmpDeviceBrand = _cursor.getString(_cursorIndexOfDeviceBrand);
            final String _tmpDeviceModel;
            _tmpDeviceModel = _cursor.getString(_cursorIndexOfDeviceModel);
            final String _tmpEntryPhotoPath;
            _tmpEntryPhotoPath = _cursor.getString(_cursorIndexOfEntryPhotoPath);
            final String _tmpCustomerName;
            _tmpCustomerName = _cursor.getString(_cursorIndexOfCustomerName);
            final String _tmpCustomerMobile;
            _tmpCustomerMobile = _cursor.getString(_cursorIndexOfCustomerMobile);
            final String _tmpCustomerCity;
            _tmpCustomerCity = _cursor.getString(_cursorIndexOfCustomerCity);
            final String _tmpDealerName;
            _tmpDealerName = _cursor.getString(_cursorIndexOfDealerName);
            final String _tmpDealerMobile;
            _tmpDealerMobile = _cursor.getString(_cursorIndexOfDealerMobile);
            final long _tmpServiceManId;
            _tmpServiceManId = _cursor.getLong(_cursorIndexOfServiceManId);
            final long _tmpEntryDate;
            _tmpEntryDate = _cursor.getLong(_cursorIndexOfEntryDate);
            final String _tmpFaultDetected;
            _tmpFaultDetected = _cursor.getString(_cursorIndexOfFaultDetected);
            final String _tmpFaultDescription;
            _tmpFaultDescription = _cursor.getString(_cursorIndexOfFaultDescription);
            final String _tmpAdditionalFaults;
            _tmpAdditionalFaults = _cursor.getString(_cursorIndexOfAdditionalFaults);
            final String _tmpInspectionPhotoPath;
            _tmpInspectionPhotoPath = _cursor.getString(_cursorIndexOfInspectionPhotoPath);
            final long _tmpInspectionDate;
            _tmpInspectionDate = _cursor.getLong(_cursorIndexOfInspectionDate);
            final boolean _tmpInspectionDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfInspectionDone);
            _tmpInspectionDone = _tmp != 0;
            final double _tmpChargeAmount;
            _tmpChargeAmount = _cursor.getDouble(_cursorIndexOfChargeAmount);
            final double _tmpAdvanceAmount;
            _tmpAdvanceAmount = _cursor.getDouble(_cursorIndexOfAdvanceAmount);
            final long _tmpQuotationDate;
            _tmpQuotationDate = _cursor.getLong(_cursorIndexOfQuotationDate);
            final boolean _tmpQuotationDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfQuotationDone);
            _tmpQuotationDone = _tmp_1 != 0;
            final String _tmpSparePartPhotoPath;
            _tmpSparePartPhotoPath = _cursor.getString(_cursorIndexOfSparePartPhotoPath);
            final String _tmpSparePartName;
            _tmpSparePartName = _cursor.getString(_cursorIndexOfSparePartName);
            final double _tmpSparePartPurchasePrice;
            _tmpSparePartPurchasePrice = _cursor.getDouble(_cursorIndexOfSparePartPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final long _tmpSparePartDate;
            _tmpSparePartDate = _cursor.getLong(_cursorIndexOfSparePartDate);
            final boolean _tmpSparePartDone;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSparePartDone);
            _tmpSparePartDone = _tmp_2 != 0;
            final String _tmpWorkStatus;
            _tmpWorkStatus = _cursor.getString(_cursorIndexOfWorkStatus);
            final long _tmpCompletionDate;
            _tmpCompletionDate = _cursor.getLong(_cursorIndexOfCompletionDate);
            final boolean _tmpWorkDone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWorkDone);
            _tmpWorkDone = _tmp_3 != 0;
            final double _tmpFinalAmount;
            _tmpFinalAmount = _cursor.getDouble(_cursorIndexOfFinalAmount);
            final String _tmpPaymentMode;
            _tmpPaymentMode = _cursor.getString(_cursorIndexOfPaymentMode);
            final double _tmpOnlineAmount;
            _tmpOnlineAmount = _cursor.getDouble(_cursorIndexOfOnlineAmount);
            final double _tmpCashAmount;
            _tmpCashAmount = _cursor.getDouble(_cursorIndexOfCashAmount);
            final long _tmpHandoverDate;
            _tmpHandoverDate = _cursor.getLong(_cursorIndexOfHandoverDate);
            final boolean _tmpHandoverDone;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfHandoverDone);
            _tmpHandoverDone = _tmp_4 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new RepairEntry(_tmpId,_tmpDeviceBrand,_tmpDeviceModel,_tmpEntryPhotoPath,_tmpCustomerName,_tmpCustomerMobile,_tmpCustomerCity,_tmpDealerName,_tmpDealerMobile,_tmpServiceManId,_tmpEntryDate,_tmpFaultDetected,_tmpFaultDescription,_tmpAdditionalFaults,_tmpInspectionPhotoPath,_tmpInspectionDate,_tmpInspectionDone,_tmpChargeAmount,_tmpAdvanceAmount,_tmpQuotationDate,_tmpQuotationDone,_tmpSparePartPhotoPath,_tmpSparePartName,_tmpSparePartPurchasePrice,_tmpSupplierId,_tmpSparePartDate,_tmpSparePartDone,_tmpWorkStatus,_tmpCompletionDate,_tmpWorkDone,_tmpFinalAmount,_tmpPaymentMode,_tmpOnlineAmount,_tmpCashAmount,_tmpHandoverDate,_tmpHandoverDone,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<List<RepairEntry>> getPendingEntries() {
    final String _sql = "SELECT * FROM repair_entries WHERE handoverDone = 0 ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repair_entries"}, new Callable<List<RepairEntry>>() {
      @Override
      @NonNull
      public List<RepairEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceBrand");
          final int _cursorIndexOfDeviceModel = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceModel");
          final int _cursorIndexOfEntryPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "entryPhotoPath");
          final int _cursorIndexOfCustomerName = CursorUtil.getColumnIndexOrThrow(_cursor, "customerName");
          final int _cursorIndexOfCustomerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "customerMobile");
          final int _cursorIndexOfCustomerCity = CursorUtil.getColumnIndexOrThrow(_cursor, "customerCity");
          final int _cursorIndexOfDealerName = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerName");
          final int _cursorIndexOfDealerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerMobile");
          final int _cursorIndexOfServiceManId = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceManId");
          final int _cursorIndexOfEntryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "entryDate");
          final int _cursorIndexOfFaultDetected = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDetected");
          final int _cursorIndexOfFaultDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDescription");
          final int _cursorIndexOfAdditionalFaults = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalFaults");
          final int _cursorIndexOfInspectionPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionPhotoPath");
          final int _cursorIndexOfInspectionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDate");
          final int _cursorIndexOfInspectionDone = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDone");
          final int _cursorIndexOfChargeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "chargeAmount");
          final int _cursorIndexOfAdvanceAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "advanceAmount");
          final int _cursorIndexOfQuotationDate = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDate");
          final int _cursorIndexOfQuotationDone = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDone");
          final int _cursorIndexOfSparePartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPhotoPath");
          final int _cursorIndexOfSparePartName = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartName");
          final int _cursorIndexOfSparePartPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPurchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSparePartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDate");
          final int _cursorIndexOfSparePartDone = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDone");
          final int _cursorIndexOfWorkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "workStatus");
          final int _cursorIndexOfCompletionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "completionDate");
          final int _cursorIndexOfWorkDone = CursorUtil.getColumnIndexOrThrow(_cursor, "workDone");
          final int _cursorIndexOfFinalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "finalAmount");
          final int _cursorIndexOfPaymentMode = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentMode");
          final int _cursorIndexOfOnlineAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "onlineAmount");
          final int _cursorIndexOfCashAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "cashAmount");
          final int _cursorIndexOfHandoverDate = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDate");
          final int _cursorIndexOfHandoverDone = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<RepairEntry> _result = new ArrayList<RepairEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RepairEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDeviceBrand;
            _tmpDeviceBrand = _cursor.getString(_cursorIndexOfDeviceBrand);
            final String _tmpDeviceModel;
            _tmpDeviceModel = _cursor.getString(_cursorIndexOfDeviceModel);
            final String _tmpEntryPhotoPath;
            _tmpEntryPhotoPath = _cursor.getString(_cursorIndexOfEntryPhotoPath);
            final String _tmpCustomerName;
            _tmpCustomerName = _cursor.getString(_cursorIndexOfCustomerName);
            final String _tmpCustomerMobile;
            _tmpCustomerMobile = _cursor.getString(_cursorIndexOfCustomerMobile);
            final String _tmpCustomerCity;
            _tmpCustomerCity = _cursor.getString(_cursorIndexOfCustomerCity);
            final String _tmpDealerName;
            _tmpDealerName = _cursor.getString(_cursorIndexOfDealerName);
            final String _tmpDealerMobile;
            _tmpDealerMobile = _cursor.getString(_cursorIndexOfDealerMobile);
            final long _tmpServiceManId;
            _tmpServiceManId = _cursor.getLong(_cursorIndexOfServiceManId);
            final long _tmpEntryDate;
            _tmpEntryDate = _cursor.getLong(_cursorIndexOfEntryDate);
            final String _tmpFaultDetected;
            _tmpFaultDetected = _cursor.getString(_cursorIndexOfFaultDetected);
            final String _tmpFaultDescription;
            _tmpFaultDescription = _cursor.getString(_cursorIndexOfFaultDescription);
            final String _tmpAdditionalFaults;
            _tmpAdditionalFaults = _cursor.getString(_cursorIndexOfAdditionalFaults);
            final String _tmpInspectionPhotoPath;
            _tmpInspectionPhotoPath = _cursor.getString(_cursorIndexOfInspectionPhotoPath);
            final long _tmpInspectionDate;
            _tmpInspectionDate = _cursor.getLong(_cursorIndexOfInspectionDate);
            final boolean _tmpInspectionDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfInspectionDone);
            _tmpInspectionDone = _tmp != 0;
            final double _tmpChargeAmount;
            _tmpChargeAmount = _cursor.getDouble(_cursorIndexOfChargeAmount);
            final double _tmpAdvanceAmount;
            _tmpAdvanceAmount = _cursor.getDouble(_cursorIndexOfAdvanceAmount);
            final long _tmpQuotationDate;
            _tmpQuotationDate = _cursor.getLong(_cursorIndexOfQuotationDate);
            final boolean _tmpQuotationDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfQuotationDone);
            _tmpQuotationDone = _tmp_1 != 0;
            final String _tmpSparePartPhotoPath;
            _tmpSparePartPhotoPath = _cursor.getString(_cursorIndexOfSparePartPhotoPath);
            final String _tmpSparePartName;
            _tmpSparePartName = _cursor.getString(_cursorIndexOfSparePartName);
            final double _tmpSparePartPurchasePrice;
            _tmpSparePartPurchasePrice = _cursor.getDouble(_cursorIndexOfSparePartPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final long _tmpSparePartDate;
            _tmpSparePartDate = _cursor.getLong(_cursorIndexOfSparePartDate);
            final boolean _tmpSparePartDone;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSparePartDone);
            _tmpSparePartDone = _tmp_2 != 0;
            final String _tmpWorkStatus;
            _tmpWorkStatus = _cursor.getString(_cursorIndexOfWorkStatus);
            final long _tmpCompletionDate;
            _tmpCompletionDate = _cursor.getLong(_cursorIndexOfCompletionDate);
            final boolean _tmpWorkDone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWorkDone);
            _tmpWorkDone = _tmp_3 != 0;
            final double _tmpFinalAmount;
            _tmpFinalAmount = _cursor.getDouble(_cursorIndexOfFinalAmount);
            final String _tmpPaymentMode;
            _tmpPaymentMode = _cursor.getString(_cursorIndexOfPaymentMode);
            final double _tmpOnlineAmount;
            _tmpOnlineAmount = _cursor.getDouble(_cursorIndexOfOnlineAmount);
            final double _tmpCashAmount;
            _tmpCashAmount = _cursor.getDouble(_cursorIndexOfCashAmount);
            final long _tmpHandoverDate;
            _tmpHandoverDate = _cursor.getLong(_cursorIndexOfHandoverDate);
            final boolean _tmpHandoverDone;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfHandoverDone);
            _tmpHandoverDone = _tmp_4 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new RepairEntry(_tmpId,_tmpDeviceBrand,_tmpDeviceModel,_tmpEntryPhotoPath,_tmpCustomerName,_tmpCustomerMobile,_tmpCustomerCity,_tmpDealerName,_tmpDealerMobile,_tmpServiceManId,_tmpEntryDate,_tmpFaultDetected,_tmpFaultDescription,_tmpAdditionalFaults,_tmpInspectionPhotoPath,_tmpInspectionDate,_tmpInspectionDone,_tmpChargeAmount,_tmpAdvanceAmount,_tmpQuotationDate,_tmpQuotationDone,_tmpSparePartPhotoPath,_tmpSparePartName,_tmpSparePartPurchasePrice,_tmpSupplierId,_tmpSparePartDate,_tmpSparePartDone,_tmpWorkStatus,_tmpCompletionDate,_tmpWorkDone,_tmpFinalAmount,_tmpPaymentMode,_tmpOnlineAmount,_tmpCashAmount,_tmpHandoverDate,_tmpHandoverDone,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<List<RepairEntry>> getCompletedEntries() {
    final String _sql = "SELECT * FROM repair_entries WHERE handoverDone = 1 ORDER BY handoverDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repair_entries"}, new Callable<List<RepairEntry>>() {
      @Override
      @NonNull
      public List<RepairEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceBrand");
          final int _cursorIndexOfDeviceModel = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceModel");
          final int _cursorIndexOfEntryPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "entryPhotoPath");
          final int _cursorIndexOfCustomerName = CursorUtil.getColumnIndexOrThrow(_cursor, "customerName");
          final int _cursorIndexOfCustomerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "customerMobile");
          final int _cursorIndexOfCustomerCity = CursorUtil.getColumnIndexOrThrow(_cursor, "customerCity");
          final int _cursorIndexOfDealerName = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerName");
          final int _cursorIndexOfDealerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerMobile");
          final int _cursorIndexOfServiceManId = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceManId");
          final int _cursorIndexOfEntryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "entryDate");
          final int _cursorIndexOfFaultDetected = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDetected");
          final int _cursorIndexOfFaultDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDescription");
          final int _cursorIndexOfAdditionalFaults = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalFaults");
          final int _cursorIndexOfInspectionPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionPhotoPath");
          final int _cursorIndexOfInspectionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDate");
          final int _cursorIndexOfInspectionDone = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDone");
          final int _cursorIndexOfChargeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "chargeAmount");
          final int _cursorIndexOfAdvanceAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "advanceAmount");
          final int _cursorIndexOfQuotationDate = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDate");
          final int _cursorIndexOfQuotationDone = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDone");
          final int _cursorIndexOfSparePartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPhotoPath");
          final int _cursorIndexOfSparePartName = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartName");
          final int _cursorIndexOfSparePartPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPurchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSparePartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDate");
          final int _cursorIndexOfSparePartDone = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDone");
          final int _cursorIndexOfWorkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "workStatus");
          final int _cursorIndexOfCompletionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "completionDate");
          final int _cursorIndexOfWorkDone = CursorUtil.getColumnIndexOrThrow(_cursor, "workDone");
          final int _cursorIndexOfFinalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "finalAmount");
          final int _cursorIndexOfPaymentMode = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentMode");
          final int _cursorIndexOfOnlineAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "onlineAmount");
          final int _cursorIndexOfCashAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "cashAmount");
          final int _cursorIndexOfHandoverDate = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDate");
          final int _cursorIndexOfHandoverDone = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<RepairEntry> _result = new ArrayList<RepairEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RepairEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDeviceBrand;
            _tmpDeviceBrand = _cursor.getString(_cursorIndexOfDeviceBrand);
            final String _tmpDeviceModel;
            _tmpDeviceModel = _cursor.getString(_cursorIndexOfDeviceModel);
            final String _tmpEntryPhotoPath;
            _tmpEntryPhotoPath = _cursor.getString(_cursorIndexOfEntryPhotoPath);
            final String _tmpCustomerName;
            _tmpCustomerName = _cursor.getString(_cursorIndexOfCustomerName);
            final String _tmpCustomerMobile;
            _tmpCustomerMobile = _cursor.getString(_cursorIndexOfCustomerMobile);
            final String _tmpCustomerCity;
            _tmpCustomerCity = _cursor.getString(_cursorIndexOfCustomerCity);
            final String _tmpDealerName;
            _tmpDealerName = _cursor.getString(_cursorIndexOfDealerName);
            final String _tmpDealerMobile;
            _tmpDealerMobile = _cursor.getString(_cursorIndexOfDealerMobile);
            final long _tmpServiceManId;
            _tmpServiceManId = _cursor.getLong(_cursorIndexOfServiceManId);
            final long _tmpEntryDate;
            _tmpEntryDate = _cursor.getLong(_cursorIndexOfEntryDate);
            final String _tmpFaultDetected;
            _tmpFaultDetected = _cursor.getString(_cursorIndexOfFaultDetected);
            final String _tmpFaultDescription;
            _tmpFaultDescription = _cursor.getString(_cursorIndexOfFaultDescription);
            final String _tmpAdditionalFaults;
            _tmpAdditionalFaults = _cursor.getString(_cursorIndexOfAdditionalFaults);
            final String _tmpInspectionPhotoPath;
            _tmpInspectionPhotoPath = _cursor.getString(_cursorIndexOfInspectionPhotoPath);
            final long _tmpInspectionDate;
            _tmpInspectionDate = _cursor.getLong(_cursorIndexOfInspectionDate);
            final boolean _tmpInspectionDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfInspectionDone);
            _tmpInspectionDone = _tmp != 0;
            final double _tmpChargeAmount;
            _tmpChargeAmount = _cursor.getDouble(_cursorIndexOfChargeAmount);
            final double _tmpAdvanceAmount;
            _tmpAdvanceAmount = _cursor.getDouble(_cursorIndexOfAdvanceAmount);
            final long _tmpQuotationDate;
            _tmpQuotationDate = _cursor.getLong(_cursorIndexOfQuotationDate);
            final boolean _tmpQuotationDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfQuotationDone);
            _tmpQuotationDone = _tmp_1 != 0;
            final String _tmpSparePartPhotoPath;
            _tmpSparePartPhotoPath = _cursor.getString(_cursorIndexOfSparePartPhotoPath);
            final String _tmpSparePartName;
            _tmpSparePartName = _cursor.getString(_cursorIndexOfSparePartName);
            final double _tmpSparePartPurchasePrice;
            _tmpSparePartPurchasePrice = _cursor.getDouble(_cursorIndexOfSparePartPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final long _tmpSparePartDate;
            _tmpSparePartDate = _cursor.getLong(_cursorIndexOfSparePartDate);
            final boolean _tmpSparePartDone;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSparePartDone);
            _tmpSparePartDone = _tmp_2 != 0;
            final String _tmpWorkStatus;
            _tmpWorkStatus = _cursor.getString(_cursorIndexOfWorkStatus);
            final long _tmpCompletionDate;
            _tmpCompletionDate = _cursor.getLong(_cursorIndexOfCompletionDate);
            final boolean _tmpWorkDone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWorkDone);
            _tmpWorkDone = _tmp_3 != 0;
            final double _tmpFinalAmount;
            _tmpFinalAmount = _cursor.getDouble(_cursorIndexOfFinalAmount);
            final String _tmpPaymentMode;
            _tmpPaymentMode = _cursor.getString(_cursorIndexOfPaymentMode);
            final double _tmpOnlineAmount;
            _tmpOnlineAmount = _cursor.getDouble(_cursorIndexOfOnlineAmount);
            final double _tmpCashAmount;
            _tmpCashAmount = _cursor.getDouble(_cursorIndexOfCashAmount);
            final long _tmpHandoverDate;
            _tmpHandoverDate = _cursor.getLong(_cursorIndexOfHandoverDate);
            final boolean _tmpHandoverDone;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfHandoverDone);
            _tmpHandoverDone = _tmp_4 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new RepairEntry(_tmpId,_tmpDeviceBrand,_tmpDeviceModel,_tmpEntryPhotoPath,_tmpCustomerName,_tmpCustomerMobile,_tmpCustomerCity,_tmpDealerName,_tmpDealerMobile,_tmpServiceManId,_tmpEntryDate,_tmpFaultDetected,_tmpFaultDescription,_tmpAdditionalFaults,_tmpInspectionPhotoPath,_tmpInspectionDate,_tmpInspectionDone,_tmpChargeAmount,_tmpAdvanceAmount,_tmpQuotationDate,_tmpQuotationDone,_tmpSparePartPhotoPath,_tmpSparePartName,_tmpSparePartPurchasePrice,_tmpSupplierId,_tmpSparePartDate,_tmpSparePartDone,_tmpWorkStatus,_tmpCompletionDate,_tmpWorkDone,_tmpFinalAmount,_tmpPaymentMode,_tmpOnlineAmount,_tmpCashAmount,_tmpHandoverDate,_tmpHandoverDone,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<List<RepairEntry>> getEntriesByServiceMan(final long serviceManId) {
    final String _sql = "SELECT * FROM repair_entries WHERE serviceManId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, serviceManId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repair_entries"}, new Callable<List<RepairEntry>>() {
      @Override
      @NonNull
      public List<RepairEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceBrand");
          final int _cursorIndexOfDeviceModel = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceModel");
          final int _cursorIndexOfEntryPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "entryPhotoPath");
          final int _cursorIndexOfCustomerName = CursorUtil.getColumnIndexOrThrow(_cursor, "customerName");
          final int _cursorIndexOfCustomerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "customerMobile");
          final int _cursorIndexOfCustomerCity = CursorUtil.getColumnIndexOrThrow(_cursor, "customerCity");
          final int _cursorIndexOfDealerName = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerName");
          final int _cursorIndexOfDealerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerMobile");
          final int _cursorIndexOfServiceManId = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceManId");
          final int _cursorIndexOfEntryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "entryDate");
          final int _cursorIndexOfFaultDetected = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDetected");
          final int _cursorIndexOfFaultDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDescription");
          final int _cursorIndexOfAdditionalFaults = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalFaults");
          final int _cursorIndexOfInspectionPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionPhotoPath");
          final int _cursorIndexOfInspectionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDate");
          final int _cursorIndexOfInspectionDone = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDone");
          final int _cursorIndexOfChargeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "chargeAmount");
          final int _cursorIndexOfAdvanceAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "advanceAmount");
          final int _cursorIndexOfQuotationDate = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDate");
          final int _cursorIndexOfQuotationDone = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDone");
          final int _cursorIndexOfSparePartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPhotoPath");
          final int _cursorIndexOfSparePartName = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartName");
          final int _cursorIndexOfSparePartPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPurchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSparePartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDate");
          final int _cursorIndexOfSparePartDone = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDone");
          final int _cursorIndexOfWorkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "workStatus");
          final int _cursorIndexOfCompletionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "completionDate");
          final int _cursorIndexOfWorkDone = CursorUtil.getColumnIndexOrThrow(_cursor, "workDone");
          final int _cursorIndexOfFinalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "finalAmount");
          final int _cursorIndexOfPaymentMode = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentMode");
          final int _cursorIndexOfOnlineAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "onlineAmount");
          final int _cursorIndexOfCashAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "cashAmount");
          final int _cursorIndexOfHandoverDate = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDate");
          final int _cursorIndexOfHandoverDone = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<RepairEntry> _result = new ArrayList<RepairEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RepairEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDeviceBrand;
            _tmpDeviceBrand = _cursor.getString(_cursorIndexOfDeviceBrand);
            final String _tmpDeviceModel;
            _tmpDeviceModel = _cursor.getString(_cursorIndexOfDeviceModel);
            final String _tmpEntryPhotoPath;
            _tmpEntryPhotoPath = _cursor.getString(_cursorIndexOfEntryPhotoPath);
            final String _tmpCustomerName;
            _tmpCustomerName = _cursor.getString(_cursorIndexOfCustomerName);
            final String _tmpCustomerMobile;
            _tmpCustomerMobile = _cursor.getString(_cursorIndexOfCustomerMobile);
            final String _tmpCustomerCity;
            _tmpCustomerCity = _cursor.getString(_cursorIndexOfCustomerCity);
            final String _tmpDealerName;
            _tmpDealerName = _cursor.getString(_cursorIndexOfDealerName);
            final String _tmpDealerMobile;
            _tmpDealerMobile = _cursor.getString(_cursorIndexOfDealerMobile);
            final long _tmpServiceManId;
            _tmpServiceManId = _cursor.getLong(_cursorIndexOfServiceManId);
            final long _tmpEntryDate;
            _tmpEntryDate = _cursor.getLong(_cursorIndexOfEntryDate);
            final String _tmpFaultDetected;
            _tmpFaultDetected = _cursor.getString(_cursorIndexOfFaultDetected);
            final String _tmpFaultDescription;
            _tmpFaultDescription = _cursor.getString(_cursorIndexOfFaultDescription);
            final String _tmpAdditionalFaults;
            _tmpAdditionalFaults = _cursor.getString(_cursorIndexOfAdditionalFaults);
            final String _tmpInspectionPhotoPath;
            _tmpInspectionPhotoPath = _cursor.getString(_cursorIndexOfInspectionPhotoPath);
            final long _tmpInspectionDate;
            _tmpInspectionDate = _cursor.getLong(_cursorIndexOfInspectionDate);
            final boolean _tmpInspectionDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfInspectionDone);
            _tmpInspectionDone = _tmp != 0;
            final double _tmpChargeAmount;
            _tmpChargeAmount = _cursor.getDouble(_cursorIndexOfChargeAmount);
            final double _tmpAdvanceAmount;
            _tmpAdvanceAmount = _cursor.getDouble(_cursorIndexOfAdvanceAmount);
            final long _tmpQuotationDate;
            _tmpQuotationDate = _cursor.getLong(_cursorIndexOfQuotationDate);
            final boolean _tmpQuotationDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfQuotationDone);
            _tmpQuotationDone = _tmp_1 != 0;
            final String _tmpSparePartPhotoPath;
            _tmpSparePartPhotoPath = _cursor.getString(_cursorIndexOfSparePartPhotoPath);
            final String _tmpSparePartName;
            _tmpSparePartName = _cursor.getString(_cursorIndexOfSparePartName);
            final double _tmpSparePartPurchasePrice;
            _tmpSparePartPurchasePrice = _cursor.getDouble(_cursorIndexOfSparePartPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final long _tmpSparePartDate;
            _tmpSparePartDate = _cursor.getLong(_cursorIndexOfSparePartDate);
            final boolean _tmpSparePartDone;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSparePartDone);
            _tmpSparePartDone = _tmp_2 != 0;
            final String _tmpWorkStatus;
            _tmpWorkStatus = _cursor.getString(_cursorIndexOfWorkStatus);
            final long _tmpCompletionDate;
            _tmpCompletionDate = _cursor.getLong(_cursorIndexOfCompletionDate);
            final boolean _tmpWorkDone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWorkDone);
            _tmpWorkDone = _tmp_3 != 0;
            final double _tmpFinalAmount;
            _tmpFinalAmount = _cursor.getDouble(_cursorIndexOfFinalAmount);
            final String _tmpPaymentMode;
            _tmpPaymentMode = _cursor.getString(_cursorIndexOfPaymentMode);
            final double _tmpOnlineAmount;
            _tmpOnlineAmount = _cursor.getDouble(_cursorIndexOfOnlineAmount);
            final double _tmpCashAmount;
            _tmpCashAmount = _cursor.getDouble(_cursorIndexOfCashAmount);
            final long _tmpHandoverDate;
            _tmpHandoverDate = _cursor.getLong(_cursorIndexOfHandoverDate);
            final boolean _tmpHandoverDone;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfHandoverDone);
            _tmpHandoverDone = _tmp_4 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new RepairEntry(_tmpId,_tmpDeviceBrand,_tmpDeviceModel,_tmpEntryPhotoPath,_tmpCustomerName,_tmpCustomerMobile,_tmpCustomerCity,_tmpDealerName,_tmpDealerMobile,_tmpServiceManId,_tmpEntryDate,_tmpFaultDetected,_tmpFaultDescription,_tmpAdditionalFaults,_tmpInspectionPhotoPath,_tmpInspectionDate,_tmpInspectionDone,_tmpChargeAmount,_tmpAdvanceAmount,_tmpQuotationDate,_tmpQuotationDone,_tmpSparePartPhotoPath,_tmpSparePartName,_tmpSparePartPurchasePrice,_tmpSupplierId,_tmpSparePartDate,_tmpSparePartDone,_tmpWorkStatus,_tmpCompletionDate,_tmpWorkDone,_tmpFinalAmount,_tmpPaymentMode,_tmpOnlineAmount,_tmpCashAmount,_tmpHandoverDate,_tmpHandoverDone,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<List<RepairEntry>> getEntriesByDateRange(final long startDate, final long endDate) {
    final String _sql = "SELECT * FROM repair_entries WHERE createdAt BETWEEN ? AND ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repair_entries"}, new Callable<List<RepairEntry>>() {
      @Override
      @NonNull
      public List<RepairEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceBrand");
          final int _cursorIndexOfDeviceModel = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceModel");
          final int _cursorIndexOfEntryPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "entryPhotoPath");
          final int _cursorIndexOfCustomerName = CursorUtil.getColumnIndexOrThrow(_cursor, "customerName");
          final int _cursorIndexOfCustomerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "customerMobile");
          final int _cursorIndexOfCustomerCity = CursorUtil.getColumnIndexOrThrow(_cursor, "customerCity");
          final int _cursorIndexOfDealerName = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerName");
          final int _cursorIndexOfDealerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerMobile");
          final int _cursorIndexOfServiceManId = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceManId");
          final int _cursorIndexOfEntryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "entryDate");
          final int _cursorIndexOfFaultDetected = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDetected");
          final int _cursorIndexOfFaultDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDescription");
          final int _cursorIndexOfAdditionalFaults = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalFaults");
          final int _cursorIndexOfInspectionPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionPhotoPath");
          final int _cursorIndexOfInspectionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDate");
          final int _cursorIndexOfInspectionDone = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDone");
          final int _cursorIndexOfChargeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "chargeAmount");
          final int _cursorIndexOfAdvanceAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "advanceAmount");
          final int _cursorIndexOfQuotationDate = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDate");
          final int _cursorIndexOfQuotationDone = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDone");
          final int _cursorIndexOfSparePartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPhotoPath");
          final int _cursorIndexOfSparePartName = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartName");
          final int _cursorIndexOfSparePartPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPurchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSparePartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDate");
          final int _cursorIndexOfSparePartDone = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDone");
          final int _cursorIndexOfWorkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "workStatus");
          final int _cursorIndexOfCompletionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "completionDate");
          final int _cursorIndexOfWorkDone = CursorUtil.getColumnIndexOrThrow(_cursor, "workDone");
          final int _cursorIndexOfFinalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "finalAmount");
          final int _cursorIndexOfPaymentMode = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentMode");
          final int _cursorIndexOfOnlineAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "onlineAmount");
          final int _cursorIndexOfCashAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "cashAmount");
          final int _cursorIndexOfHandoverDate = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDate");
          final int _cursorIndexOfHandoverDone = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<RepairEntry> _result = new ArrayList<RepairEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RepairEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDeviceBrand;
            _tmpDeviceBrand = _cursor.getString(_cursorIndexOfDeviceBrand);
            final String _tmpDeviceModel;
            _tmpDeviceModel = _cursor.getString(_cursorIndexOfDeviceModel);
            final String _tmpEntryPhotoPath;
            _tmpEntryPhotoPath = _cursor.getString(_cursorIndexOfEntryPhotoPath);
            final String _tmpCustomerName;
            _tmpCustomerName = _cursor.getString(_cursorIndexOfCustomerName);
            final String _tmpCustomerMobile;
            _tmpCustomerMobile = _cursor.getString(_cursorIndexOfCustomerMobile);
            final String _tmpCustomerCity;
            _tmpCustomerCity = _cursor.getString(_cursorIndexOfCustomerCity);
            final String _tmpDealerName;
            _tmpDealerName = _cursor.getString(_cursorIndexOfDealerName);
            final String _tmpDealerMobile;
            _tmpDealerMobile = _cursor.getString(_cursorIndexOfDealerMobile);
            final long _tmpServiceManId;
            _tmpServiceManId = _cursor.getLong(_cursorIndexOfServiceManId);
            final long _tmpEntryDate;
            _tmpEntryDate = _cursor.getLong(_cursorIndexOfEntryDate);
            final String _tmpFaultDetected;
            _tmpFaultDetected = _cursor.getString(_cursorIndexOfFaultDetected);
            final String _tmpFaultDescription;
            _tmpFaultDescription = _cursor.getString(_cursorIndexOfFaultDescription);
            final String _tmpAdditionalFaults;
            _tmpAdditionalFaults = _cursor.getString(_cursorIndexOfAdditionalFaults);
            final String _tmpInspectionPhotoPath;
            _tmpInspectionPhotoPath = _cursor.getString(_cursorIndexOfInspectionPhotoPath);
            final long _tmpInspectionDate;
            _tmpInspectionDate = _cursor.getLong(_cursorIndexOfInspectionDate);
            final boolean _tmpInspectionDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfInspectionDone);
            _tmpInspectionDone = _tmp != 0;
            final double _tmpChargeAmount;
            _tmpChargeAmount = _cursor.getDouble(_cursorIndexOfChargeAmount);
            final double _tmpAdvanceAmount;
            _tmpAdvanceAmount = _cursor.getDouble(_cursorIndexOfAdvanceAmount);
            final long _tmpQuotationDate;
            _tmpQuotationDate = _cursor.getLong(_cursorIndexOfQuotationDate);
            final boolean _tmpQuotationDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfQuotationDone);
            _tmpQuotationDone = _tmp_1 != 0;
            final String _tmpSparePartPhotoPath;
            _tmpSparePartPhotoPath = _cursor.getString(_cursorIndexOfSparePartPhotoPath);
            final String _tmpSparePartName;
            _tmpSparePartName = _cursor.getString(_cursorIndexOfSparePartName);
            final double _tmpSparePartPurchasePrice;
            _tmpSparePartPurchasePrice = _cursor.getDouble(_cursorIndexOfSparePartPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final long _tmpSparePartDate;
            _tmpSparePartDate = _cursor.getLong(_cursorIndexOfSparePartDate);
            final boolean _tmpSparePartDone;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSparePartDone);
            _tmpSparePartDone = _tmp_2 != 0;
            final String _tmpWorkStatus;
            _tmpWorkStatus = _cursor.getString(_cursorIndexOfWorkStatus);
            final long _tmpCompletionDate;
            _tmpCompletionDate = _cursor.getLong(_cursorIndexOfCompletionDate);
            final boolean _tmpWorkDone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWorkDone);
            _tmpWorkDone = _tmp_3 != 0;
            final double _tmpFinalAmount;
            _tmpFinalAmount = _cursor.getDouble(_cursorIndexOfFinalAmount);
            final String _tmpPaymentMode;
            _tmpPaymentMode = _cursor.getString(_cursorIndexOfPaymentMode);
            final double _tmpOnlineAmount;
            _tmpOnlineAmount = _cursor.getDouble(_cursorIndexOfOnlineAmount);
            final double _tmpCashAmount;
            _tmpCashAmount = _cursor.getDouble(_cursorIndexOfCashAmount);
            final long _tmpHandoverDate;
            _tmpHandoverDate = _cursor.getLong(_cursorIndexOfHandoverDate);
            final boolean _tmpHandoverDone;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfHandoverDone);
            _tmpHandoverDone = _tmp_4 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new RepairEntry(_tmpId,_tmpDeviceBrand,_tmpDeviceModel,_tmpEntryPhotoPath,_tmpCustomerName,_tmpCustomerMobile,_tmpCustomerCity,_tmpDealerName,_tmpDealerMobile,_tmpServiceManId,_tmpEntryDate,_tmpFaultDetected,_tmpFaultDescription,_tmpAdditionalFaults,_tmpInspectionPhotoPath,_tmpInspectionDate,_tmpInspectionDone,_tmpChargeAmount,_tmpAdvanceAmount,_tmpQuotationDate,_tmpQuotationDone,_tmpSparePartPhotoPath,_tmpSparePartName,_tmpSparePartPurchasePrice,_tmpSupplierId,_tmpSparePartDate,_tmpSparePartDone,_tmpWorkStatus,_tmpCompletionDate,_tmpWorkDone,_tmpFinalAmount,_tmpPaymentMode,_tmpOnlineAmount,_tmpCashAmount,_tmpHandoverDate,_tmpHandoverDone,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<Integer> getPendingCount() {
    final String _sql = "SELECT COUNT(*) FROM repair_entries WHERE handoverDone = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repair_entries"}, new Callable<Integer>() {
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

  @Override
  public Flow<Integer> getCompletedCountInRange(final long startDate, final long endDate) {
    final String _sql = "SELECT COUNT(*) FROM repair_entries WHERE handoverDone = 1 AND handoverDate BETWEEN ? AND ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repair_entries"}, new Callable<Integer>() {
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

  @Override
  public Flow<Double> getRevenueInRange(final long startDate, final long endDate) {
    final String _sql = "SELECT SUM(finalAmount) FROM repair_entries WHERE handoverDone = 1 AND handoverDate BETWEEN ? AND ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repair_entries"}, new Callable<Double>() {
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
  public Flow<List<DailyReportRow>> getDailyReport(final long startDate, final long endDate) {
    final String _sql = "\n"
            + "        SELECT strftime('%Y-%m-%d', handoverDate / 1000, 'unixepoch') as dateGroup,\n"
            + "               COUNT(*) as count,\n"
            + "               SUM(finalAmount) as totalRevenue\n"
            + "        FROM repair_entries \n"
            + "        WHERE handoverDone = 1 \n"
            + "          AND handoverDate BETWEEN ? AND ?\n"
            + "        GROUP BY dateGroup\n"
            + "        ORDER BY dateGroup ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repair_entries"}, new Callable<List<DailyReportRow>>() {
      @Override
      @NonNull
      public List<DailyReportRow> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDateGroup = 0;
          final int _cursorIndexOfCount = 1;
          final int _cursorIndexOfTotalRevenue = 2;
          final List<DailyReportRow> _result = new ArrayList<DailyReportRow>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyReportRow _item;
            final String _tmpDateGroup;
            _tmpDateGroup = _cursor.getString(_cursorIndexOfDateGroup);
            final int _tmpCount;
            _tmpCount = _cursor.getInt(_cursorIndexOfCount);
            final Double _tmpTotalRevenue;
            if (_cursor.isNull(_cursorIndexOfTotalRevenue)) {
              _tmpTotalRevenue = null;
            } else {
              _tmpTotalRevenue = _cursor.getDouble(_cursorIndexOfTotalRevenue);
            }
            _item = new DailyReportRow(_tmpDateGroup,_tmpCount,_tmpTotalRevenue);
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
  public Flow<List<RepairEntry>> searchEntries(final String query) {
    final String _sql = "SELECT * FROM repair_entries WHERE customerName LIKE '%' || ? || '%' OR customerMobile LIKE '%' || ? || '%' OR dealerName LIKE '%' || ? || '%'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    _argIndex = 3;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"repair_entries"}, new Callable<List<RepairEntry>>() {
      @Override
      @NonNull
      public List<RepairEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDeviceBrand = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceBrand");
          final int _cursorIndexOfDeviceModel = CursorUtil.getColumnIndexOrThrow(_cursor, "deviceModel");
          final int _cursorIndexOfEntryPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "entryPhotoPath");
          final int _cursorIndexOfCustomerName = CursorUtil.getColumnIndexOrThrow(_cursor, "customerName");
          final int _cursorIndexOfCustomerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "customerMobile");
          final int _cursorIndexOfCustomerCity = CursorUtil.getColumnIndexOrThrow(_cursor, "customerCity");
          final int _cursorIndexOfDealerName = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerName");
          final int _cursorIndexOfDealerMobile = CursorUtil.getColumnIndexOrThrow(_cursor, "dealerMobile");
          final int _cursorIndexOfServiceManId = CursorUtil.getColumnIndexOrThrow(_cursor, "serviceManId");
          final int _cursorIndexOfEntryDate = CursorUtil.getColumnIndexOrThrow(_cursor, "entryDate");
          final int _cursorIndexOfFaultDetected = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDetected");
          final int _cursorIndexOfFaultDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "faultDescription");
          final int _cursorIndexOfAdditionalFaults = CursorUtil.getColumnIndexOrThrow(_cursor, "additionalFaults");
          final int _cursorIndexOfInspectionPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionPhotoPath");
          final int _cursorIndexOfInspectionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDate");
          final int _cursorIndexOfInspectionDone = CursorUtil.getColumnIndexOrThrow(_cursor, "inspectionDone");
          final int _cursorIndexOfChargeAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "chargeAmount");
          final int _cursorIndexOfAdvanceAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "advanceAmount");
          final int _cursorIndexOfQuotationDate = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDate");
          final int _cursorIndexOfQuotationDone = CursorUtil.getColumnIndexOrThrow(_cursor, "quotationDone");
          final int _cursorIndexOfSparePartPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPhotoPath");
          final int _cursorIndexOfSparePartName = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartName");
          final int _cursorIndexOfSparePartPurchasePrice = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartPurchasePrice");
          final int _cursorIndexOfSupplierId = CursorUtil.getColumnIndexOrThrow(_cursor, "supplierId");
          final int _cursorIndexOfSparePartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDate");
          final int _cursorIndexOfSparePartDone = CursorUtil.getColumnIndexOrThrow(_cursor, "sparePartDone");
          final int _cursorIndexOfWorkStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "workStatus");
          final int _cursorIndexOfCompletionDate = CursorUtil.getColumnIndexOrThrow(_cursor, "completionDate");
          final int _cursorIndexOfWorkDone = CursorUtil.getColumnIndexOrThrow(_cursor, "workDone");
          final int _cursorIndexOfFinalAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "finalAmount");
          final int _cursorIndexOfPaymentMode = CursorUtil.getColumnIndexOrThrow(_cursor, "paymentMode");
          final int _cursorIndexOfOnlineAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "onlineAmount");
          final int _cursorIndexOfCashAmount = CursorUtil.getColumnIndexOrThrow(_cursor, "cashAmount");
          final int _cursorIndexOfHandoverDate = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDate");
          final int _cursorIndexOfHandoverDone = CursorUtil.getColumnIndexOrThrow(_cursor, "handoverDone");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<RepairEntry> _result = new ArrayList<RepairEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final RepairEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpDeviceBrand;
            _tmpDeviceBrand = _cursor.getString(_cursorIndexOfDeviceBrand);
            final String _tmpDeviceModel;
            _tmpDeviceModel = _cursor.getString(_cursorIndexOfDeviceModel);
            final String _tmpEntryPhotoPath;
            _tmpEntryPhotoPath = _cursor.getString(_cursorIndexOfEntryPhotoPath);
            final String _tmpCustomerName;
            _tmpCustomerName = _cursor.getString(_cursorIndexOfCustomerName);
            final String _tmpCustomerMobile;
            _tmpCustomerMobile = _cursor.getString(_cursorIndexOfCustomerMobile);
            final String _tmpCustomerCity;
            _tmpCustomerCity = _cursor.getString(_cursorIndexOfCustomerCity);
            final String _tmpDealerName;
            _tmpDealerName = _cursor.getString(_cursorIndexOfDealerName);
            final String _tmpDealerMobile;
            _tmpDealerMobile = _cursor.getString(_cursorIndexOfDealerMobile);
            final long _tmpServiceManId;
            _tmpServiceManId = _cursor.getLong(_cursorIndexOfServiceManId);
            final long _tmpEntryDate;
            _tmpEntryDate = _cursor.getLong(_cursorIndexOfEntryDate);
            final String _tmpFaultDetected;
            _tmpFaultDetected = _cursor.getString(_cursorIndexOfFaultDetected);
            final String _tmpFaultDescription;
            _tmpFaultDescription = _cursor.getString(_cursorIndexOfFaultDescription);
            final String _tmpAdditionalFaults;
            _tmpAdditionalFaults = _cursor.getString(_cursorIndexOfAdditionalFaults);
            final String _tmpInspectionPhotoPath;
            _tmpInspectionPhotoPath = _cursor.getString(_cursorIndexOfInspectionPhotoPath);
            final long _tmpInspectionDate;
            _tmpInspectionDate = _cursor.getLong(_cursorIndexOfInspectionDate);
            final boolean _tmpInspectionDone;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfInspectionDone);
            _tmpInspectionDone = _tmp != 0;
            final double _tmpChargeAmount;
            _tmpChargeAmount = _cursor.getDouble(_cursorIndexOfChargeAmount);
            final double _tmpAdvanceAmount;
            _tmpAdvanceAmount = _cursor.getDouble(_cursorIndexOfAdvanceAmount);
            final long _tmpQuotationDate;
            _tmpQuotationDate = _cursor.getLong(_cursorIndexOfQuotationDate);
            final boolean _tmpQuotationDone;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfQuotationDone);
            _tmpQuotationDone = _tmp_1 != 0;
            final String _tmpSparePartPhotoPath;
            _tmpSparePartPhotoPath = _cursor.getString(_cursorIndexOfSparePartPhotoPath);
            final String _tmpSparePartName;
            _tmpSparePartName = _cursor.getString(_cursorIndexOfSparePartName);
            final double _tmpSparePartPurchasePrice;
            _tmpSparePartPurchasePrice = _cursor.getDouble(_cursorIndexOfSparePartPurchasePrice);
            final long _tmpSupplierId;
            _tmpSupplierId = _cursor.getLong(_cursorIndexOfSupplierId);
            final long _tmpSparePartDate;
            _tmpSparePartDate = _cursor.getLong(_cursorIndexOfSparePartDate);
            final boolean _tmpSparePartDone;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfSparePartDone);
            _tmpSparePartDone = _tmp_2 != 0;
            final String _tmpWorkStatus;
            _tmpWorkStatus = _cursor.getString(_cursorIndexOfWorkStatus);
            final long _tmpCompletionDate;
            _tmpCompletionDate = _cursor.getLong(_cursorIndexOfCompletionDate);
            final boolean _tmpWorkDone;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfWorkDone);
            _tmpWorkDone = _tmp_3 != 0;
            final double _tmpFinalAmount;
            _tmpFinalAmount = _cursor.getDouble(_cursorIndexOfFinalAmount);
            final String _tmpPaymentMode;
            _tmpPaymentMode = _cursor.getString(_cursorIndexOfPaymentMode);
            final double _tmpOnlineAmount;
            _tmpOnlineAmount = _cursor.getDouble(_cursorIndexOfOnlineAmount);
            final double _tmpCashAmount;
            _tmpCashAmount = _cursor.getDouble(_cursorIndexOfCashAmount);
            final long _tmpHandoverDate;
            _tmpHandoverDate = _cursor.getLong(_cursorIndexOfHandoverDate);
            final boolean _tmpHandoverDone;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfHandoverDone);
            _tmpHandoverDone = _tmp_4 != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new RepairEntry(_tmpId,_tmpDeviceBrand,_tmpDeviceModel,_tmpEntryPhotoPath,_tmpCustomerName,_tmpCustomerMobile,_tmpCustomerCity,_tmpDealerName,_tmpDealerMobile,_tmpServiceManId,_tmpEntryDate,_tmpFaultDetected,_tmpFaultDescription,_tmpAdditionalFaults,_tmpInspectionPhotoPath,_tmpInspectionDate,_tmpInspectionDone,_tmpChargeAmount,_tmpAdvanceAmount,_tmpQuotationDate,_tmpQuotationDone,_tmpSparePartPhotoPath,_tmpSparePartName,_tmpSparePartPurchasePrice,_tmpSupplierId,_tmpSparePartDate,_tmpSparePartDone,_tmpWorkStatus,_tmpCompletionDate,_tmpWorkDone,_tmpFinalAmount,_tmpPaymentMode,_tmpOnlineAmount,_tmpCashAmount,_tmpHandoverDate,_tmpHandoverDone,_tmpCreatedAt,_tmpUpdatedAt);
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
