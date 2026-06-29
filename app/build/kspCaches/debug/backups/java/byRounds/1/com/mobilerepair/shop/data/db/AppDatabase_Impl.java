package com.mobilerepair.shop.data.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.mobilerepair.shop.data.db.dao.CommonFaultDao;
import com.mobilerepair.shop.data.db.dao.CommonFaultDao_Impl;
import com.mobilerepair.shop.data.db.dao.CustomerDao;
import com.mobilerepair.shop.data.db.dao.CustomerDao_Impl;
import com.mobilerepair.shop.data.db.dao.DealerDao;
import com.mobilerepair.shop.data.db.dao.DealerDao_Impl;
import com.mobilerepair.shop.data.db.dao.RepairEntryDao;
import com.mobilerepair.shop.data.db.dao.RepairEntryDao_Impl;
import com.mobilerepair.shop.data.db.dao.SaleDao;
import com.mobilerepair.shop.data.db.dao.SaleDao_Impl;
import com.mobilerepair.shop.data.db.dao.ServiceManDao;
import com.mobilerepair.shop.data.db.dao.ServiceManDao_Impl;
import com.mobilerepair.shop.data.db.dao.SparePartPurchaseDao;
import com.mobilerepair.shop.data.db.dao.SparePartPurchaseDao_Impl;
import com.mobilerepair.shop.data.db.dao.SupplierDao;
import com.mobilerepair.shop.data.db.dao.SupplierDao_Impl;
import com.mobilerepair.shop.data.db.dao.UserProfileDao;
import com.mobilerepair.shop.data.db.dao.UserProfileDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile RepairEntryDao _repairEntryDao;

  private volatile ServiceManDao _serviceManDao;

  private volatile SupplierDao _supplierDao;

  private volatile CommonFaultDao _commonFaultDao;

  private volatile SparePartPurchaseDao _sparePartPurchaseDao;

  private volatile CustomerDao _customerDao;

  private volatile DealerDao _dealerDao;

  private volatile SaleDao _saleDao;

  private volatile UserProfileDao _userProfileDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(5) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `repair_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `deviceBrand` TEXT NOT NULL, `deviceModel` TEXT NOT NULL, `entryPhotoPath` TEXT NOT NULL, `customerName` TEXT NOT NULL, `customerMobile` TEXT NOT NULL, `customerCity` TEXT NOT NULL, `dealerName` TEXT NOT NULL, `dealerMobile` TEXT NOT NULL, `serviceManId` INTEGER NOT NULL, `entryDate` INTEGER NOT NULL, `faultDetected` TEXT NOT NULL, `faultDescription` TEXT NOT NULL, `additionalFaults` TEXT NOT NULL, `inspectionPhotoPath` TEXT NOT NULL, `inspectionDate` INTEGER NOT NULL, `inspectionDone` INTEGER NOT NULL, `chargeAmount` REAL NOT NULL, `advanceAmount` REAL NOT NULL, `quotationDate` INTEGER NOT NULL, `quotationDone` INTEGER NOT NULL, `sparePartPhotoPath` TEXT NOT NULL, `sparePartName` TEXT NOT NULL, `sparePartPurchasePrice` REAL NOT NULL, `supplierId` INTEGER NOT NULL, `sparePartDate` INTEGER NOT NULL, `sparePartDone` INTEGER NOT NULL, `workStatus` TEXT NOT NULL, `completionDate` INTEGER NOT NULL, `workDone` INTEGER NOT NULL, `finalAmount` REAL NOT NULL, `paymentMode` TEXT NOT NULL, `onlineAmount` REAL NOT NULL, `cashAmount` REAL NOT NULL, `handoverDate` INTEGER NOT NULL, `handoverDone` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `service_men` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `mobile` TEXT NOT NULL, `email` TEXT NOT NULL, `employeeId` TEXT NOT NULL, `designation` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `suppliers` (`mobile` TEXT NOT NULL, `name` TEXT NOT NULL, `companyName` TEXT NOT NULL, `email` TEXT NOT NULL, `address` TEXT NOT NULL, `city` TEXT NOT NULL, `gstNo` TEXT NOT NULL, `suppliesTypes` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`mobile`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `common_faults` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `faultName` TEXT NOT NULL, `category` TEXT NOT NULL, `defaultCharge` REAL NOT NULL, `description` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `spare_part_purchases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `repairEntryId` INTEGER NOT NULL, `partName` TEXT NOT NULL, `partPhotoPath` TEXT NOT NULL, `purchasePrice` REAL NOT NULL, `supplierId` TEXT NOT NULL, `supplierName` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `purchaseDate` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `customers` (`mobileNumber` TEXT NOT NULL, `name` TEXT, `city` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`mobileNumber`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `dealers` (`mobileNumber` TEXT NOT NULL, `name` TEXT, `city` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`mobileNumber`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sales` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemName` TEXT NOT NULL, `supplierId` TEXT NOT NULL, `purchasePrice` REAL NOT NULL, `salePrice` REAL NOT NULL, `saleDate` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `user_profile` (`id` INTEGER NOT NULL, `email` TEXT NOT NULL, `name` TEXT NOT NULL, `phone` TEXT NOT NULL, `shopName` TEXT NOT NULL, `shopAddress` TEXT NOT NULL, `lastSyncTimestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a1a74dec12b88268d0ce7fe25f025b29')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `repair_entries`");
        db.execSQL("DROP TABLE IF EXISTS `service_men`");
        db.execSQL("DROP TABLE IF EXISTS `suppliers`");
        db.execSQL("DROP TABLE IF EXISTS `common_faults`");
        db.execSQL("DROP TABLE IF EXISTS `spare_part_purchases`");
        db.execSQL("DROP TABLE IF EXISTS `customers`");
        db.execSQL("DROP TABLE IF EXISTS `dealers`");
        db.execSQL("DROP TABLE IF EXISTS `sales`");
        db.execSQL("DROP TABLE IF EXISTS `user_profile`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsRepairEntries = new HashMap<String, TableInfo.Column>(38);
        _columnsRepairEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("deviceBrand", new TableInfo.Column("deviceBrand", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("deviceModel", new TableInfo.Column("deviceModel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("entryPhotoPath", new TableInfo.Column("entryPhotoPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("customerName", new TableInfo.Column("customerName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("customerMobile", new TableInfo.Column("customerMobile", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("customerCity", new TableInfo.Column("customerCity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("dealerName", new TableInfo.Column("dealerName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("dealerMobile", new TableInfo.Column("dealerMobile", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("serviceManId", new TableInfo.Column("serviceManId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("entryDate", new TableInfo.Column("entryDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("faultDetected", new TableInfo.Column("faultDetected", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("faultDescription", new TableInfo.Column("faultDescription", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("additionalFaults", new TableInfo.Column("additionalFaults", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("inspectionPhotoPath", new TableInfo.Column("inspectionPhotoPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("inspectionDate", new TableInfo.Column("inspectionDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("inspectionDone", new TableInfo.Column("inspectionDone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("chargeAmount", new TableInfo.Column("chargeAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("advanceAmount", new TableInfo.Column("advanceAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("quotationDate", new TableInfo.Column("quotationDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("quotationDone", new TableInfo.Column("quotationDone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("sparePartPhotoPath", new TableInfo.Column("sparePartPhotoPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("sparePartName", new TableInfo.Column("sparePartName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("sparePartPurchasePrice", new TableInfo.Column("sparePartPurchasePrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("supplierId", new TableInfo.Column("supplierId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("sparePartDate", new TableInfo.Column("sparePartDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("sparePartDone", new TableInfo.Column("sparePartDone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("workStatus", new TableInfo.Column("workStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("completionDate", new TableInfo.Column("completionDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("workDone", new TableInfo.Column("workDone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("finalAmount", new TableInfo.Column("finalAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("paymentMode", new TableInfo.Column("paymentMode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("onlineAmount", new TableInfo.Column("onlineAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("cashAmount", new TableInfo.Column("cashAmount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("handoverDate", new TableInfo.Column("handoverDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("handoverDone", new TableInfo.Column("handoverDone", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsRepairEntries.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysRepairEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesRepairEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoRepairEntries = new TableInfo("repair_entries", _columnsRepairEntries, _foreignKeysRepairEntries, _indicesRepairEntries);
        final TableInfo _existingRepairEntries = TableInfo.read(db, "repair_entries");
        if (!_infoRepairEntries.equals(_existingRepairEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "repair_entries(com.mobilerepair.shop.data.model.RepairEntry).\n"
                  + " Expected:\n" + _infoRepairEntries + "\n"
                  + " Found:\n" + _existingRepairEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsServiceMen = new HashMap<String, TableInfo.Column>(8);
        _columnsServiceMen.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceMen.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceMen.put("mobile", new TableInfo.Column("mobile", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceMen.put("email", new TableInfo.Column("email", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceMen.put("employeeId", new TableInfo.Column("employeeId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceMen.put("designation", new TableInfo.Column("designation", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceMen.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsServiceMen.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysServiceMen = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesServiceMen = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoServiceMen = new TableInfo("service_men", _columnsServiceMen, _foreignKeysServiceMen, _indicesServiceMen);
        final TableInfo _existingServiceMen = TableInfo.read(db, "service_men");
        if (!_infoServiceMen.equals(_existingServiceMen)) {
          return new RoomOpenHelper.ValidationResult(false, "service_men(com.mobilerepair.shop.data.model.ServiceMan).\n"
                  + " Expected:\n" + _infoServiceMen + "\n"
                  + " Found:\n" + _existingServiceMen);
        }
        final HashMap<String, TableInfo.Column> _columnsSuppliers = new HashMap<String, TableInfo.Column>(10);
        _columnsSuppliers.put("mobile", new TableInfo.Column("mobile", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("companyName", new TableInfo.Column("companyName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("email", new TableInfo.Column("email", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("address", new TableInfo.Column("address", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("city", new TableInfo.Column("city", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("gstNo", new TableInfo.Column("gstNo", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("suppliesTypes", new TableInfo.Column("suppliesTypes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSuppliers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSuppliers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSuppliers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSuppliers = new TableInfo("suppliers", _columnsSuppliers, _foreignKeysSuppliers, _indicesSuppliers);
        final TableInfo _existingSuppliers = TableInfo.read(db, "suppliers");
        if (!_infoSuppliers.equals(_existingSuppliers)) {
          return new RoomOpenHelper.ValidationResult(false, "suppliers(com.mobilerepair.shop.data.model.Supplier).\n"
                  + " Expected:\n" + _infoSuppliers + "\n"
                  + " Found:\n" + _existingSuppliers);
        }
        final HashMap<String, TableInfo.Column> _columnsCommonFaults = new HashMap<String, TableInfo.Column>(8);
        _columnsCommonFaults.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonFaults.put("faultName", new TableInfo.Column("faultName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonFaults.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonFaults.put("defaultCharge", new TableInfo.Column("defaultCharge", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonFaults.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonFaults.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonFaults.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCommonFaults.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCommonFaults = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCommonFaults = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCommonFaults = new TableInfo("common_faults", _columnsCommonFaults, _foreignKeysCommonFaults, _indicesCommonFaults);
        final TableInfo _existingCommonFaults = TableInfo.read(db, "common_faults");
        if (!_infoCommonFaults.equals(_existingCommonFaults)) {
          return new RoomOpenHelper.ValidationResult(false, "common_faults(com.mobilerepair.shop.data.model.CommonFault).\n"
                  + " Expected:\n" + _infoCommonFaults + "\n"
                  + " Found:\n" + _existingCommonFaults);
        }
        final HashMap<String, TableInfo.Column> _columnsSparePartPurchases = new HashMap<String, TableInfo.Column>(10);
        _columnsSparePartPurchases.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSparePartPurchases.put("repairEntryId", new TableInfo.Column("repairEntryId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSparePartPurchases.put("partName", new TableInfo.Column("partName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSparePartPurchases.put("partPhotoPath", new TableInfo.Column("partPhotoPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSparePartPurchases.put("purchasePrice", new TableInfo.Column("purchasePrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSparePartPurchases.put("supplierId", new TableInfo.Column("supplierId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSparePartPurchases.put("supplierName", new TableInfo.Column("supplierName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSparePartPurchases.put("quantity", new TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSparePartPurchases.put("purchaseDate", new TableInfo.Column("purchaseDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSparePartPurchases.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSparePartPurchases = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSparePartPurchases = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSparePartPurchases = new TableInfo("spare_part_purchases", _columnsSparePartPurchases, _foreignKeysSparePartPurchases, _indicesSparePartPurchases);
        final TableInfo _existingSparePartPurchases = TableInfo.read(db, "spare_part_purchases");
        if (!_infoSparePartPurchases.equals(_existingSparePartPurchases)) {
          return new RoomOpenHelper.ValidationResult(false, "spare_part_purchases(com.mobilerepair.shop.data.model.SparePartPurchase).\n"
                  + " Expected:\n" + _infoSparePartPurchases + "\n"
                  + " Found:\n" + _existingSparePartPurchases);
        }
        final HashMap<String, TableInfo.Column> _columnsCustomers = new HashMap<String, TableInfo.Column>(4);
        _columnsCustomers.put("mobileNumber", new TableInfo.Column("mobileNumber", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("city", new TableInfo.Column("city", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCustomers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCustomers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCustomers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoCustomers = new TableInfo("customers", _columnsCustomers, _foreignKeysCustomers, _indicesCustomers);
        final TableInfo _existingCustomers = TableInfo.read(db, "customers");
        if (!_infoCustomers.equals(_existingCustomers)) {
          return new RoomOpenHelper.ValidationResult(false, "customers(com.mobilerepair.shop.data.model.Customer).\n"
                  + " Expected:\n" + _infoCustomers + "\n"
                  + " Found:\n" + _existingCustomers);
        }
        final HashMap<String, TableInfo.Column> _columnsDealers = new HashMap<String, TableInfo.Column>(4);
        _columnsDealers.put("mobileNumber", new TableInfo.Column("mobileNumber", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDealers.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDealers.put("city", new TableInfo.Column("city", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDealers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDealers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDealers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDealers = new TableInfo("dealers", _columnsDealers, _foreignKeysDealers, _indicesDealers);
        final TableInfo _existingDealers = TableInfo.read(db, "dealers");
        if (!_infoDealers.equals(_existingDealers)) {
          return new RoomOpenHelper.ValidationResult(false, "dealers(com.mobilerepair.shop.data.model.Dealer).\n"
                  + " Expected:\n" + _infoDealers + "\n"
                  + " Found:\n" + _existingDealers);
        }
        final HashMap<String, TableInfo.Column> _columnsSales = new HashMap<String, TableInfo.Column>(6);
        _columnsSales.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSales.put("itemName", new TableInfo.Column("itemName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSales.put("supplierId", new TableInfo.Column("supplierId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSales.put("purchasePrice", new TableInfo.Column("purchasePrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSales.put("salePrice", new TableInfo.Column("salePrice", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSales.put("saleDate", new TableInfo.Column("saleDate", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSales = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSales = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSales = new TableInfo("sales", _columnsSales, _foreignKeysSales, _indicesSales);
        final TableInfo _existingSales = TableInfo.read(db, "sales");
        if (!_infoSales.equals(_existingSales)) {
          return new RoomOpenHelper.ValidationResult(false, "sales(com.mobilerepair.shop.data.model.Sale).\n"
                  + " Expected:\n" + _infoSales + "\n"
                  + " Found:\n" + _existingSales);
        }
        final HashMap<String, TableInfo.Column> _columnsUserProfile = new HashMap<String, TableInfo.Column>(7);
        _columnsUserProfile.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("email", new TableInfo.Column("email", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("phone", new TableInfo.Column("phone", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("shopName", new TableInfo.Column("shopName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("shopAddress", new TableInfo.Column("shopAddress", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUserProfile.put("lastSyncTimestamp", new TableInfo.Column("lastSyncTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUserProfile = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUserProfile = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUserProfile = new TableInfo("user_profile", _columnsUserProfile, _foreignKeysUserProfile, _indicesUserProfile);
        final TableInfo _existingUserProfile = TableInfo.read(db, "user_profile");
        if (!_infoUserProfile.equals(_existingUserProfile)) {
          return new RoomOpenHelper.ValidationResult(false, "user_profile(com.mobilerepair.shop.data.model.UserProfile).\n"
                  + " Expected:\n" + _infoUserProfile + "\n"
                  + " Found:\n" + _existingUserProfile);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "a1a74dec12b88268d0ce7fe25f025b29", "8944633120808b2ad0f11504595ed5fc");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "repair_entries","service_men","suppliers","common_faults","spare_part_purchases","customers","dealers","sales","user_profile");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `repair_entries`");
      _db.execSQL("DELETE FROM `service_men`");
      _db.execSQL("DELETE FROM `suppliers`");
      _db.execSQL("DELETE FROM `common_faults`");
      _db.execSQL("DELETE FROM `spare_part_purchases`");
      _db.execSQL("DELETE FROM `customers`");
      _db.execSQL("DELETE FROM `dealers`");
      _db.execSQL("DELETE FROM `sales`");
      _db.execSQL("DELETE FROM `user_profile`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(RepairEntryDao.class, RepairEntryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ServiceManDao.class, ServiceManDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SupplierDao.class, SupplierDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CommonFaultDao.class, CommonFaultDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SparePartPurchaseDao.class, SparePartPurchaseDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CustomerDao.class, CustomerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DealerDao.class, DealerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SaleDao.class, SaleDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(UserProfileDao.class, UserProfileDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public RepairEntryDao repairEntryDao() {
    if (_repairEntryDao != null) {
      return _repairEntryDao;
    } else {
      synchronized(this) {
        if(_repairEntryDao == null) {
          _repairEntryDao = new RepairEntryDao_Impl(this);
        }
        return _repairEntryDao;
      }
    }
  }

  @Override
  public ServiceManDao serviceManDao() {
    if (_serviceManDao != null) {
      return _serviceManDao;
    } else {
      synchronized(this) {
        if(_serviceManDao == null) {
          _serviceManDao = new ServiceManDao_Impl(this);
        }
        return _serviceManDao;
      }
    }
  }

  @Override
  public SupplierDao supplierDao() {
    if (_supplierDao != null) {
      return _supplierDao;
    } else {
      synchronized(this) {
        if(_supplierDao == null) {
          _supplierDao = new SupplierDao_Impl(this);
        }
        return _supplierDao;
      }
    }
  }

  @Override
  public CommonFaultDao commonFaultDao() {
    if (_commonFaultDao != null) {
      return _commonFaultDao;
    } else {
      synchronized(this) {
        if(_commonFaultDao == null) {
          _commonFaultDao = new CommonFaultDao_Impl(this);
        }
        return _commonFaultDao;
      }
    }
  }

  @Override
  public SparePartPurchaseDao sparePartPurchaseDao() {
    if (_sparePartPurchaseDao != null) {
      return _sparePartPurchaseDao;
    } else {
      synchronized(this) {
        if(_sparePartPurchaseDao == null) {
          _sparePartPurchaseDao = new SparePartPurchaseDao_Impl(this);
        }
        return _sparePartPurchaseDao;
      }
    }
  }

  @Override
  public CustomerDao customerDao() {
    if (_customerDao != null) {
      return _customerDao;
    } else {
      synchronized(this) {
        if(_customerDao == null) {
          _customerDao = new CustomerDao_Impl(this);
        }
        return _customerDao;
      }
    }
  }

  @Override
  public DealerDao dealerDao() {
    if (_dealerDao != null) {
      return _dealerDao;
    } else {
      synchronized(this) {
        if(_dealerDao == null) {
          _dealerDao = new DealerDao_Impl(this);
        }
        return _dealerDao;
      }
    }
  }

  @Override
  public SaleDao saleDao() {
    if (_saleDao != null) {
      return _saleDao;
    } else {
      synchronized(this) {
        if(_saleDao == null) {
          _saleDao = new SaleDao_Impl(this);
        }
        return _saleDao;
      }
    }
  }

  @Override
  public UserProfileDao userProfileDao() {
    if (_userProfileDao != null) {
      return _userProfileDao;
    } else {
      synchronized(this) {
        if(_userProfileDao == null) {
          _userProfileDao = new UserProfileDao_Impl(this);
        }
        return _userProfileDao;
      }
    }
  }
}
