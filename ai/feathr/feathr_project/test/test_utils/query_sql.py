import psycopg2
from feathr.utils._env_config_reader import EnvConfigReader

# script to query SQL database for debugging purpose

def show_table(cursor, table_name):
    cursor.execute("select * from " + table_name + ";")
    print(cursor.fetchall())

    q = """
    SELECT column_name, data_type, is_nullable
    FROM information_schema.columns
    WHERE table_name = %s;
    """

    cur = conn.cursor()
    cur.execute(q, (table_name,))  # (table_name,) passed as tuple
    print(cur.fetchall())


# Update connection string information
host = "featuremonitoring.postgres.database.azure.com"
dbname = "postgres"
user = "demo"
env_config = EnvConfigReader(config_path=None)
password = env_config.get_from_env_or_akv('SQL_TEST_PASSWORD')
sslmode = "require"

# Construct connection string
conn_string = "host={0} user={1} dbname={2} password={3} sslmode={4}".format(host, user, dbname, password, sslmode)
conn = psycopg2.connect(conn_string)
print("Connection established")

cursor = conn.cursor()

show_table(cursor, "f_int")
cursor.execute("select * from f_location_avg_fare;")
print(cursor.fetchall())


# Clean up
conn.commit()
cursor.close()
conn.close()
