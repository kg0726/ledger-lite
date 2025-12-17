-- code가 유니크라는 전제(우리가 이미 그렇게 쓰고 있으니)로,
--    "없으면 생성, 있으면 유지/갱신"하는 idempotent seed

MERGE INTO account (code, name) KEY(code) VALUES ('1000', 'CASH');
MERGE INTO account (code, name) KEY(code) VALUES ('1111', 'PRODUCT');
