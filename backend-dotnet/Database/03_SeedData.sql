-- =====================================================================
-- DINDORI PRANIT YADNYIKI & VASTU SEVA
-- INITIAL SEED DATA (MS SQL SERVER)
-- Populates the default Pooja lists and System Settings
-- =====================================================================

-- 1. Insert Default Pooja List (यज्ञिकी विभाग)
INSERT INTO Poojas (Id, Name, Category, BasePrice, SevaType, Status) VALUES
('satyanarayan_pooja', N'श्री सत्यनारायण महापूजा', N'कौटुंबिक पूजा', 1100.00, 'Yadnyiki', 'Active'),
('ganpati_pooja', N'श्री गणेश पूजा व अभिषेक', N'कौटुंबिक पूजा', 1500.00, 'Yadnyiki', 'Active'),
('vastushanti_vidhi', N'वास्तूशांत विधी व होम', N'गृह शांत विधी', 5100.00, 'Yadnyiki', 'Active'),
('rudrabhishek_havan', N'लघु रुद्र व हव्हन विधी', N'यज्ञ विधी', 7500.00, 'Yadnyiki', 'Active'),
('navchandi_yajna', N'नवचंडी महायज्ञ विधी', N'महायज्ञ विधी', 15000.00, 'Yadnyiki', 'Active'),
('lagna_vidhi', N'विवाह संस्कार विधी', N'संस्कार विधी', 5500.00, 'Yadnyiki', 'Active');

-- 2. Insert Default Vastu Sevas (वास्तू सेवा विभाग)
INSERT INTO Poojas (Id, Name, Category, BasePrice, SevaType, Status) VALUES
('vastu_tapasani_flat', N'निवासी वास्तू तपासणी (Flat/Bungalow)', N'वास्तू तपासणी', 2100.00, 'Vastu', 'Active'),
('vastu_tapasani_comm', N'व्यावसायिक वास्तू तपासणी (Shop/Office)', N'वास्तू तपासणी', 3100.00, 'Vastu', 'Active'),
('vastu_tapasani_ind', N'औद्योगिक वास्तू तपासणी (Factory)', N'वास्तू तपासणी', 5100.00, 'Vastu', 'Active'),
('vastu_upay_nodemolish', N'तोडफोड विना वास्तू दोष निवारण उपाय', N'वास्तू दोष उपाय', 1500.00, 'Vastu', 'Active');

-- 3. Insert Default System Settings (सिस्टीम पॉलिसी आणि कमिशन रेशो)
-- Default Share split split: Trust 70%, Guruji 30%
INSERT INTO SystemSettings (SettingKey, SettingValue) VALUES
('financial_config', N'{"trustSharePercent":70.0,"gurujiSharePercent":30.0,"tdsPercent":5.0,"razorpayFeePercent":2.36}'),
('system_config', N'{"maintenanceMode":false,"killSwitchEnabled":false,"disableNewBookings":false,"autoApproveServiceRequests":true,"autoProcessLowRiskWithdrawals":true}');

-- 4. Create Default Admin Owner Whitelist Account
-- (Replace this email with your actual Admin Email address)
INSERT INTO Admins (Uid, Email, Status) VALUES
('admin_owner_uid_placeholder', 'admin@dindori.org', 'Active');
