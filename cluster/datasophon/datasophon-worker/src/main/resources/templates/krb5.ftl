# Configuration snippets may be placed in this directory as well
includedir /etc/krb5.conf.d/

[logging]
 default = FILE:/var/log/krb5libs.log
 kdc = FILE:/var/log/krb5kdc.log
 admin_server = FILE:/var/log/kadmind.log

[libdefaults]
 dns_lookup_realm = false
 ticket_lifetime = ${ticketLifetime}
 renew_lifetime = ${renewLifetime}
 forwardable = true
 rdns = false
 pkinit_anchors = FILE:/etc/pki/tls/certs/ca-bundle.crt
 default_realm = ${realm}
 #default_ccache_name = KEYRING:persistent:%{uid}

[realms]
 ${realm} = {
  kdc = ${kdcHost}
  admin_server = ${kadminHost}
 }

[domain_realm]
# .example.com = ${realm}
# example.com = ${realm}
