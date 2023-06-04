Server {
 com.sun.security.auth.module.Krb5LoginModule required
 useKeyTab=true
 keyTab="/etc/security/keytab/zkserver.service.keytab"
 storeKey=true
 useTicketCache=false
 principal="zookeeper/${hostname}@${zkRealm}";
};
Client {
 com.sun.security.auth.module.Krb5LoginModule required
 useKeyTab=true
 keyTab="/etc/security/keytab/zkclient.service.keytab"
 storeKey=true
 useTicketCache=false
 principal="zkcli/${hostname}@${zkRealm}";
};
