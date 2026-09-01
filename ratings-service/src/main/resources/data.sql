-- Review dates are relative to the current date so the 30 day and quarterly report
-- windows always return rows, whenever this is run.
MERGE INTO ratings KEY(issuer_id) VALUES ('NG-1001', 'Alderney Power Holdings', 'A-', 'stable', 'Utilities', DATEADD('DAY', -9, CURRENT_DATE));
MERGE INTO ratings KEY(issuer_id) VALUES ('NG-1002', 'Bramfield Logistics', 'BBB', 'negative', 'Transport', DATEADD('DAY', -21, CURRENT_DATE));
MERGE INTO ratings KEY(issuer_id) VALUES ('NG-1003', 'Carrow Bank plc', 'AA-', 'stable', 'Financials', DATEADD('DAY', -3, CURRENT_DATE));
MERGE INTO ratings KEY(issuer_id) VALUES ('NG-1004', 'Deepgate Chemicals', 'BB+', 'watch', 'Materials', DATEADD('DAY', -54, CURRENT_DATE));
MERGE INTO ratings KEY(issuer_id) VALUES ('NG-1005', 'Eastmark Telecom', 'A', 'positive', 'Telecoms', DATEADD('DAY', -28, CURRENT_DATE));
MERGE INTO ratings KEY(issuer_id) VALUES ('NG-1006', 'Fennhaven Retail Group', 'B', 'negative', 'Consumer', DATEADD('DAY', -77, CURRENT_DATE));
MERGE INTO ratings KEY(issuer_id) VALUES ('NG-1007', 'Glenmoor Water', 'AA', 'stable', 'Utilities', DATEADD('DAY', -14, CURRENT_DATE));
MERGE INTO ratings KEY(issuer_id) VALUES ('NG-1008', 'Harlow Aerospace', 'BBB-', 'stable', 'Industrials', DATEADD('DAY', -140, CURRENT_DATE));
