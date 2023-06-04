KafkaServer {
 com.sun.security.auth.module.Krb5LoginModule required
 useKeyTab=true
 storeKey=true
 keyTab="/etc/security/keytab/kafka.service.keytab"
 principal="kafka/${kafkaHost}@${kafkaRealm}";
};
KafkaClient {
 com.sun.security.auth.module.Krb5LoginModule required
 useKeyTab=true
 useTicketCache=true
 keyTab="/etc/security/keytab/kafka.service.keytab"
 principal="kafka/${kafkaHost}@${kafkaRealm}";
};
Client {
 com.sun.security.auth.module.Krb5LoginModule required
 useKeyTab=true
 useTicketCache=true
 keyTab="/etc/security/keytab/kafka.service.keytab"
 principal="kafka/${kafkaHost}@${kafkaRealm}";
};