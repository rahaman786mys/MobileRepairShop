# Tally Integration & Photo Visibility Fix

This plan fixes the photo visibility issues (Inspection/Profile), ensures sales data is correctly reflected in ledgers, and adds profile editing for all personas.

## User Review Required

- No breaking changes or design trade-offs identified.

## Proposed Changes

### [Photo Visibility]
Fixing Glide loading and URI handling for Profile and Inspection.

#### [ProfileFragment.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/ui/profile/ProfileFragment.kt)
- Standardize Glide loading with error placeholders.
- Fix URI to Path conversion for persistence.

#### [EntryFragment.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/ui/entry/EntryFragment.kt)
- Fix photo preview logic for Photo 1 and Photo 2.

---

### [Tally-Style Ledger Integration]
Linking Direct Sales to Supplier/Customer ledgers and fixing "Sales visible in Dues" logic.

#### [SaleFragment.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/ui/sales/SaleFragment.kt)
- Update `saveSale` to create `Payment` and `PaymentTransaction` records.
- Ensure Direct Sales data correctly appears in Supplier history.

#### [SupplierDetailFragment.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/ui/master/suppliers/SupplierDetailFragment.kt)
- Add "Direct Sales History" section to the supplier ledger.

---

### [Profile Editing]
Adding "Edit" functionality to Master records.

#### [SupplierDetailFragment.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/ui/master/suppliers/SupplierDetailFragment.kt)
- Add "Edit Profile" button to the header.
- Link to a modified `SupplierAddFragment` in "Edit mode".

#### [CustomerDetailFragment.kt](file:///C:/Users/rhmna/OneDrive/Desktop/testing%202/MobileRepairShop/app/src/main/java/com/app/muzzutech/ui/master/customers/CustomerDetailFragment.kt)
- Add "Edit Profile" button.

---

## Verification Plan

### Automated Tests
- `app:assembleRelease` to ensure no resource or code regressions.

### Manual Verification
- Take profile photo -> Navigate away -> Return -> Verify photo is visible.
- Record a Direct Sale -> Check Supplier Ledger -> Verify sale appears in history.
- Check Dashboard "Dues" -> Verify only unpaid items are counted.
- Click "Edit Profile" on a Customer -> Change Name -> Verify update in ledger.
