/* -*- mode: c; c-basic-offset: 4; indent-tabs-mode: nil -*- */
/* lib/krb5/krb/pac_sign.c */
/*
 * Copyright 2008 by the Massachusetts Institute of Technology.
 * All Rights Reserved.
 *
 * Export of this software from the United States of America may
 *   require a specific license from the United States Government.
 *   It is the responsibility of any person or organization contemplating
 *   export to obtain such a license before exporting.
 *
 * WITHIN THAT CONSTRAINT, permission to use, copy, modify, and
 * distribute this software and its documentation for any purpose and
 * without fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright notice and
 * this permission notice appear in supporting documentation, and that
 * the name of M.I.T. not be used in advertising or publicity pertaining
 * to distribution of the software without specific, written prior
 * permission.  Furthermore if you modify this software you must label
 * your software as modified software and not distribute it in such a
 * fashion that it might be confused with the original M.I.T. software.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is" without express
 * or implied warranty.
 */

#include "k5-int.h"
#include "int-proto.h"
#include "authdata.h"

/* draft-brezak-win2k-krb-authz-00 */

static krb5_error_code
k5_insert_client_info(krb5_context context,
                      krb5_pac pac,
                      krb5_timestamp authtime,
                      krb5_const_principal principal,
                      krb5_boolean with_realm)
{
    krb5_error_code ret;
    krb5_data client_info;
    char *princ_name_utf8 = NULL;
    unsigned char *princ_name_utf16 = NULL, *p;
    size_t princ_name_utf16_len = 0;
    uint64_t nt_authtime;
    int flags = 0;

    /* If we already have a CLIENT_INFO buffer, then just validate it */
    if (k5_pac_locate_buffer(context, pac, KRB5_PAC_CLIENT_INFO,
                             &client_info) == 0) {
        return k5_pac_validate_client(context, pac, authtime, principal,
                                      with_realm);
    }

    if (!with_realm) {
        flags |= KRB5_PRINCIPAL_UNPARSE_NO_REALM;
    } else if (principal->type == KRB5_NT_ENTERPRISE_PRINCIPAL) {
        /* Avoid quoting the first @ sign for enterprise name with realm. */
        flags |= KRB5_PRINCIPAL_UNPARSE_DISPLAY;
    }

    ret = krb5_unparse_name_flags(context, principal, flags, &princ_name_utf8);
    if (ret != 0)
        goto cleanup;

    ret = k5_utf8_to_utf16le(princ_name_utf8, &princ_name_utf16,
                             &princ_name_utf16_len);
    if (ret != 0)
        goto cleanup;

    client_info.length = PAC_CLIENT_INFO_LENGTH + princ_name_utf16_len;
    client_info.data = NULL;

    ret = k5_pac_add_buffer(context, pac, KRB5_PAC_CLIENT_INFO,
                            &client_info, TRUE, &client_info);
    if (ret != 0)
        goto cleanup;

    p = (unsigned char *)client_info.data;

    /* copy in authtime converted to a 64-bit NT time */
    k5_seconds_since_1970_to_time(authtime, &nt_authtime);
    store_64_le(nt_authtime, p);
    p += 8;

    /* copy in number of UTF-16 bytes in principal name */
    store_16_le(princ_name_utf16_len, p);
    p += 2;

    /* copy in principal name */
    memcpy(p, princ_name_utf16, princ_name_utf16_len);

cleanup:
    if (princ_name_utf16 != NULL)
        free(princ_name_utf16);
    krb5_free_unparsed_name(context, princ_name_utf8);

    return ret;
}

static krb5_error_code
k5_insert_checksum(krb5_context context,
                   krb5_pac pac,
                   krb5_ui_4 type,
                   const krb5_keyblock *key,
                   krb5_cksumtype *cksumtype)
{
    krb5_error_code ret;
    size_t len;
    krb5_data cksumdata;

    ret = krb5int_c_mandatory_cksumtype(context, key->enctype, cksumtype);
    if (ret != 0)
        return ret;

    ret = krb5_c_checksum_length(context, *cksumtype, &len);
    if (ret != 0)
        return ret;

    ret = k5_pac_locate_buffer(context, pac, type, &cksumdata);
    if (ret == 0) {
        /*
         * If we're resigning PAC, make sure we can fit checksum
         * into existing buffer
         */
        if (cksumdata.length != PAC_SIGNATURE_DATA_LENGTH + len)
            return ERANGE;

        memset(cksumdata.data, 0, cksumdata.length);
    } else {
        /* Add a zero filled buffer */
        cksumdata.length = PAC_SIGNATURE_DATA_LENGTH + len;
        cksumdata.data = NULL;

        ret = k5_pac_add_buffer(context, pac,
                                type, &cksumdata,
                                TRUE, &cksumdata);
        if (ret != 0)
            return ret;
    }

    /* Encode checksum type into buffer */
    store_32_le((krb5_ui_4)*cksumtype, cksumdata.data);

    return 0;
}

/* in-place encoding of PAC header */
static krb5_error_code
k5_pac_encode_header(krb5_context context, krb5_pac pac)
{
    size_t i;
    unsigned char *p;
    size_t header_len;

    header_len = PACTYPE_LENGTH +
        (pac->pac->cBuffers * PAC_INFO_BUFFER_LENGTH);
    assert(pac->data.length >= header_len);

    p = (unsigned char *)pac->data.data;

    store_32_le(pac->pac->cBuffers, p);
    p += 4;
    store_32_le(pac->pac->Version, p);
    p += 4;

    for (i = 0; i < pac->pac->cBuffers; i++) {
        PAC_INFO_BUFFER *buffer = &pac->pac->Buffers[i];

        store_32_le(buffer->ulType, p);
        p += 4;
        store_32_le(buffer->cbBufferSize, p);
        p += 4;
        store_64_le(buffer->Offset, p);
        p += 8;

        assert((buffer->Offset % PAC_ALIGNMENT) == 0);
        assert(buffer->Offset + buffer->cbBufferSize <= pac->data.length);
        assert(buffer->Offset >= header_len);

        if (buffer->Offset % PAC_ALIGNMENT ||
            buffer->Offset + buffer->cbBufferSize > pac->data.length ||
            buffer->Offset < header_len)
            return ERANGE;
    }

    return 0;
}

krb5_error_code KRB5_CALLCONV
krb5_pac_sign(krb5_context context, krb5_pac pac, krb5_timestamp authtime,
              krb5_const_principal principal, const krb5_keyblock *server_key,
              const krb5_keyblock *privsvr_key, krb5_data *data)
{
    return krb5_pac_sign_ext(context, pac, authtime, principal, server_key,
                             privsvr_key, FALSE, data);
}

krb5_error_code KRB5_CALLCONV
krb5_pac_sign_ext(krb5_context context, krb5_pac pac, krb5_timestamp authtime,
                  krb5_const_principal principal,
                  const krb5_keyblock *server_key,
                  const krb5_keyblock *privsvr_key, krb5_boolean with_realm,
                  krb5_data *data)
{
    krb5_error_code ret;
    krb5_data server_cksum, privsvr_cksum;
    krb5_cksumtype server_cksumtype, privsvr_cksumtype;
    krb5_crypto_iov iov[2];

    data->length = 0;
    data->data = NULL;

    if (principal != NULL) {
        ret = k5_insert_client_info(context, pac, authtime, principal,
                                    with_realm);
        if (ret != 0)
            return ret;
    }

    /* Create zeroed buffers for both checksums */
    ret = k5_insert_checksum(context, pac, KRB5_PAC_SERVER_CHECKSUM,
                             server_key, &server_cksumtype);
    if (ret != 0)
        return ret;

    ret = k5_insert_checksum(context, pac, KRB5_PAC_PRIVSVR_CHECKSUM,
                             privsvr_key, &privsvr_cksumtype);
    if (ret != 0)
        return ret;

    /* Now, encode the PAC header so that the checksums will include it */
    ret = k5_pac_encode_header(context, pac);
    if (ret != 0)
        return ret;

    /* Generate the server checksum over the entire PAC */
    ret = k5_pac_locate_buffer(context, pac, KRB5_PAC_SERVER_CHECKSUM,
                               &server_cksum);
    if (ret != 0)
        return ret;

    assert(server_cksum.length > PAC_SIGNATURE_DATA_LENGTH);

    iov[0].flags = KRB5_CRYPTO_TYPE_DATA;
    iov[0].data = pac->data;

    iov[1].flags = KRB5_CRYPTO_TYPE_CHECKSUM;
    iov[1].data.data = server_cksum.data + PAC_SIGNATURE_DATA_LENGTH;
    iov[1].data.length = server_cksum.length - PAC_SIGNATURE_DATA_LENGTH;

    ret = krb5_c_make_checksum_iov(context, server_cksumtype,
                                   server_key, KRB5_KEYUSAGE_APP_DATA_CKSUM,
                                   iov, sizeof(iov)/sizeof(iov[0]));
    if (ret != 0)
        return ret;

    /* Generate the privsvr checksum over the server checksum buffer */
    ret = k5_pac_locate_buffer(context, pac, KRB5_PAC_PRIVSVR_CHECKSUM,
                               &privsvr_cksum);
    if (ret != 0)
        return ret;

    assert(privsvr_cksum.length > PAC_SIGNATURE_DATA_LENGTH);

    iov[0].flags = KRB5_CRYPTO_TYPE_DATA;
    iov[0].data.data = server_cksum.data + PAC_SIGNATURE_DATA_LENGTH;
    iov[0].data.length = server_cksum.length - PAC_SIGNATURE_DATA_LENGTH;

    iov[1].flags = KRB5_CRYPTO_TYPE_CHECKSUM;
    iov[1].data.data = privsvr_cksum.data + PAC_SIGNATURE_DATA_LENGTH;
    iov[1].data.length = privsvr_cksum.length - PAC_SIGNATURE_DATA_LENGTH;

    ret = krb5_c_make_checksum_iov(context, privsvr_cksumtype,
                                   privsvr_key, KRB5_KEYUSAGE_APP_DATA_CKSUM,
                                   iov, sizeof(iov)/sizeof(iov[0]));
    if (ret != 0)
        return ret;

    data->data = k5memdup(pac->data.data, pac->data.length, &ret);
    if (data->data == NULL)
        return ret;
    data->length = pac->data.length;

    memset(pac->data.data, 0,
           PACTYPE_LENGTH + (pac->pac->cBuffers * PAC_INFO_BUFFER_LENGTH));

    return 0;
}

/* Add a signature over der_enc_tkt in privsvr to pac.  der_enc_tkt should be
 * encoded with a dummy PAC authdata element containing a single zero byte. */
static krb5_error_code
add_ticket_signature(krb5_context context, const krb5_pac pac,
                     krb5_data *der_enc_tkt, const krb5_keyblock *privsvr)
{
    krb5_error_code ret;
    krb5_data ticket_cksum;
    krb5_cksumtype ticket_cksumtype;
    krb5_crypto_iov iov[2];

    /* Create zeroed buffer for checksum. */
    ret = k5_insert_checksum(context, pac, KRB5_PAC_TICKET_CHECKSUM,
                             privsvr, &ticket_cksumtype);
    if (ret)
        return ret;

    ret = k5_pac_locate_buffer(context, pac, KRB5_PAC_TICKET_CHECKSUM,
                               &ticket_cksum);
    if (ret)
        return ret;

    iov[0].flags = KRB5_CRYPTO_TYPE_DATA;
    iov[0].data = *der_enc_tkt;
    iov[1].flags = KRB5_CRYPTO_TYPE_CHECKSUM;
    iov[1].data = make_data(ticket_cksum.data + PAC_SIGNATURE_DATA_LENGTH,
                            ticket_cksum.length - PAC_SIGNATURE_DATA_LENGTH);
    ret = krb5_c_make_checksum_iov(context, ticket_cksumtype, privsvr,
                                   KRB5_KEYUSAGE_APP_DATA_CKSUM, iov, 2);
    if (ret)
        return ret;

    store_32_le(ticket_cksumtype, ticket_cksum.data);
    return 0;
}

/* Set *out to an AD-IF-RELEVANT authdata element containing a PAC authdata
 * element with contents pac_data. */
static krb5_error_code
encode_pac_ad(krb5_context context, krb5_data *pac_data, krb5_authdata **out)
{
    krb5_error_code ret;
    krb5_authdata *container[2], **encoded_container = NULL;
    krb5_authdata pac_ad = { KV5M_AUTHDATA, KRB5_AUTHDATA_WIN2K_PAC };
    uint8_t z = 0;

    pac_ad.contents = (pac_data != NULL) ? (uint8_t *)pac_data->data : &z;
    pac_ad.length = (pac_data != NULL) ? pac_data->length : 1;
    container[0] = &pac_ad;
    container[1] = NULL;

    ret = krb5_encode_authdata_container(context, KRB5_AUTHDATA_IF_RELEVANT,
                                         container, &encoded_container);
    if (ret)
        return ret;

    *out = encoded_container[0];
    free(encoded_container);
    return 0;
}

krb5_error_code KRB5_CALLCONV
krb5_kdc_sign_ticket(krb5_context context, krb5_enc_tkt_part *enc_tkt,
                     const krb5_pac pac, krb5_const_principal server_princ,
                     krb5_const_principal client_princ,
                     const krb5_keyblock *server, const krb5_keyblock *privsvr,
                     krb5_boolean with_realm)
{
    krb5_error_code ret;
    krb5_data *der_enc_tkt = NULL, pac_data = empty_data();
    krb5_authdata **list, *pac_ad;
    size_t count;

    /* Reallocate space for another authdata element in enc_tkt. */
    list = enc_tkt->authorization_data;
    for (count = 0; list != NULL && list[count] != NULL; count++);
    list = realloc(enc_tkt->authorization_data, (count + 2) * sizeof(*list));
    if (list == NULL)
        return ENOMEM;
    list[count] = NULL;
    enc_tkt->authorization_data = list;

    /* Create a dummy PAC for ticket signing and make it the first element. */
    ret = encode_pac_ad(context, NULL, &pac_ad);
    if (ret)
        goto cleanup;
    memmove(list + 1, list, (count + 1) * sizeof(*list));
    list[0] = pac_ad;

    if (k5_pac_should_have_ticket_signature(server_princ)) {
        ret = encode_krb5_enc_tkt_part(enc_tkt, &der_enc_tkt);
        if (ret)
            goto cleanup;

        assert(privsvr != NULL);
        ret = add_ticket_signature(context, pac, der_enc_tkt, privsvr);
        if (ret)
            goto cleanup;
    }

    ret = krb5_pac_sign_ext(context, pac, enc_tkt->times.authtime,
                            client_princ, server, privsvr, with_realm,
                            &pac_data);
    if (ret)
        goto cleanup;

    /* Replace the dummy PAC with the signed real one. */
    ret = encode_pac_ad(context, &pac_data, &pac_ad);
    if (ret)
        goto cleanup;
    free(list[0]->contents);
    free(list[0]);
    list[0] = pac_ad;

cleanup:
    krb5_free_data(context, der_enc_tkt);
    krb5_free_data_contents(context, &pac_data);
    return ret;
}
