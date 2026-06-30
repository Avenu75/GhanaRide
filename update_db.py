import pymysql

try:
    connection = pymysql.connect(
        host='localhost',
        user='root',
        password='Avenubright75$',
        database='ghanaride',
        port=3307
    )

    with connection.cursor() as cursor:
        cursor.execute("ALTER TABLE trips MODIFY status VARCHAR(50);")
        cursor.execute("ALTER TABLE bookings MODIFY status VARCHAR(50);")
        cursor.execute("ALTER TABLE bookings MODIFY booking_type VARCHAR(50);")
        cursor.execute("ALTER TABLE bookings MODIFY payment_method VARCHAR(50);")
        cursor.execute("ALTER TABLE bookings MODIFY payment_status VARCHAR(50);")
    connection.commit()
    print("Database columns successfully updated to VARCHAR(50).")
except Exception as e:
    print(f"Error: {e}")
finally:
    if 'connection' in locals() and connection.open:
        connection.close()
