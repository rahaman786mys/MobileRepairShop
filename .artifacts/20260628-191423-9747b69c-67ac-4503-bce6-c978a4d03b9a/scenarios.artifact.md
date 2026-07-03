# 1000 Test Scenarios for MuZZu Tech

## Category 1: HAPPY PATH (150)
1. **Login**: Complete OTP login via WhatsApp.
2. **Dashboard**: Navigate from Dashboard to New Entry.
3. **New Entry**: Create a new customer repair entry with a photo.
4. **New Entry**: Create a new dealer repair entry.
5. **Inspection**: Record faults detected in a repair entry.
6. **Quotation**: Set charge amount and advance for a repair.
7. **Spare Parts**: Add a part purchase from a supplier for a repair.
8. **Handover**: Complete a repair and record payment (Cash).
9. **Handover**: Complete a repair and record payment (Online).
10. **Quick Sale**: Record a direct sale of an item.
11. **Master Data**: Add a new Service Man.
12. **Master Data**: Add a new Supplier.
13. **Master Data**: Add a new Common Fault.
14. **Customer Ledger**: View history for a specific customer.
15. **Supplier Ledger**: View dues for a specific supplier.
16. **Reports**: View daily revenue report.
17. **Dues**: Record a partial payment for a pending due.
18. **Payroll**: Mark attendance for a technician.
19. **Payroll**: Generate salary slip for a month.
20. **Expenses**: Add a recurring shop expense.
... (130 more)

## Category 2: BOUNDARY & INPUT EDGE CASES (200)
1. **Empty Fields**: Submit New Entry with all fields blank.
2. **Short Mobile**: Enter 5-digit number in login.
3. **Long Mobile**: Enter 15-digit number in login.
4. **Huge Amount**: Enter Rs. 1,000,000,000 in Quotation.
5. **Negative Amount**: Attempt to enter -500 in Spare Parts price.
6. **Zero Price**: Set Quotation amount to 0.
7. **Special Characters**: Use `'; DROP TABLE customers; --` in customer name.
8. **Emojis**: Use 🔥📱 in fault description.
9. **Whitespace**: Submit a name as "   ".
10. **Long Text**: Enter 5000 characters in Fault Description.
... (190 more)

## Category 3: NAVIGATION & STATE CHAOS (150)
1. **Back Spam**: Rapidly tap back from Handover to Dashboard.
2. **Rotation**: Rotate screen while photo is being captured.
3. **Backgrounding**: Press Home button while saving a Sale.
4. **Force Kill**: Kill app mid-Quotation and resume.
5. **Abandonment**: Fill half of New Entry, go back, verify draft state.
... (145 more)

## Category 4: CONCURRENCY & TIMING (100)
1. **Double Tap**: Tap "Save Entry" twice within 50ms.
2. **Race Condition**: Try to record two payments for the same due simultaneously.
... (98 more)

## Category 5: DATA INTEGRITY ADVERSARIAL (100)
1. **Orphan Check**: Delete a Service Man who has active repairs assigned.
2. **Negative Inventory**: Try to return more parts than purchased.
... (98 more)

## Category 6: SECURITY & AUTH ADVERSARIAL (80)
1. **Biometric Bypass**: Cancel biometric prompt, check access.
2. **Deep Link**: Attempt to open Dashboard without being logged in.
... (78 more)

## Category 7: PERMISSION & ENVIRONMENT FAILURES (70)
1. **Camera Deny**: Deny camera permission when taking entry photo.
2. **Storage Full**: Simulate storage full during database write.
... (68 more)

## Category 8: PERFORMANCE & SCALE (70)
1. **Bulk Data**: View All Entries with 10,000 records.
2. **Large Reports**: Generate reports for a 5-year date range.
... (68 more)

## Category 9: VISUAL & ACCESSIBILITY (50)
1. **Dark Mode**: Verify all screens in Dark Mode.
2. **Small Screen**: Check touch targets on a 4-inch screen.
... (48 more)

## Category 10: MULTI-STEP COMBINATORIAL FLOWS (30)
1. **Full Lifecycle**: Supplier Add -> Purchase Part -> Customer Add -> Entry -> Inspection -> Part Assign -> Handover -> Check Reports -> Check Dues.
... (29 more)
