/* -*- mode: c; c-basic-offset: 4; indent-tabs-mode: nil -*- */
/* kdc/do_tgs_req.c - KDC Routines to deal with TGS_REQ's */
/*
 * Copyright 1990, 1991, 2001, 2007, 2008, 2009, 2013, 2014 by the
 * Massachusetts Institute of Technology.  All Rights Reserved.
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
/*
 * Copyright (c) 2006-2008, Novell, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *       this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *   * The copyright holder's name is not used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

#include "k5-int.h"

#include <syslog.h>
#ifdef HAVE_NETINET_IN_H
#include <sys/types.h>
#include <netinet/in.h>
#ifndef hpux
#include <arpa/inet.h>
#endif
#endif

#include "kdc_util.h"
#include "kdc_audit.h"
#include "policy.h"
#include "extern.h"
#include "adm_proto.h"
#include <ctype.h>

static krb5_error_code
find_alternate_tgs(kdc_realm_t *, krb5_principal, krb5_db_entry **,
                   const char**);

static krb5_error_code
prepare_error_tgs(struct kdc_request_state *, krb5_kdc_req *,krb5_ticket *,int,
                  krb5_principal,krb5_data **,const char *, krb5_pa_data **);

static krb5_error_code
decrypt_2ndtkt(kdc_realm_t *, krb5_kdc_req *, krb5_flags, krb5_db_entry *,
               krb5_keyblock *, const krb5_ticket **, krb5_pac *,
               krb5_db_entry **, krb5_keyblock **, const char **);

static krb5_error_code
gen_session_key(kdc_realm_t *, krb5_kdc_req *, krb5_db_entry *,
                krb5_keyblock *, const char **);

static krb5_int32
find_referral_tgs(kdc_realm_t *, krb5_kdc_req *, krb5_principal *);

static krb5_error_code
db_get_svc_princ(krb5_context, krb5_principal, krb5_flags,
                 krb5_db_entry **, const char **);

static krb5_error_code
search_sprinc(kdc_realm_t *, krb5_kdc_req *, krb5_flags,
              krb5_db_entry **, const char **);

/*ARGSUSED*/
krb5_error_code
process_tgs_req(krb5_kdc_req *request, krb5_data *pkt,
                const krb5_fulladdr *from, kdc_realm_t *kdc_active_realm,
                krb5_data **response)
{
    krb5_keyblock * subkey = 0;
    krb5_keyblock *header_key = NULL;
    krb5_keyblock *stkt_server_key = NULL;
    krb5_keyblock *subject_key;
    krb5_db_entry *server = NULL;
    krb5_db_entry *stkt_server = NULL;
    krb5_db_entry *subject_server;
    krb5_kdc_rep reply;
    krb5_enc_kdc_rep_part reply_encpart;
    krb5_ticket ticket_reply, *header_ticket = 0;
    const krb5_ticket *stkt = NULL;
    krb5_enc_tkt_part enc_tkt_reply;
    int newtransited = 0;
    krb5_error_code retval = 0;
    krb5_keyblock server_keyblock, *encrypting_key;
    krb5_timestamp kdc_time, authtime = 0;
    krb5_keyblock session_key, local_tgt_key;
    krb5_keyblock *reply_key = NULL;
    krb5_principal cprinc = NULL, sprinc = NULL, altcprinc = NULL;
    krb5_principal stkt_authdata_client = NULL;
    krb5_last_req_entry *nolrarray[2], nolrentry;
    int errcode;
    const char        *status = 0;
    krb5_enc_tkt_part *header_enc_tkt = NULL; /* TGT */
    krb5_enc_tkt_part *subject_tkt = NULL; /* TGT or evidence ticket */
    krb5_db_entry *client = NULL, *header_server = NULL;
    krb5_db_entry *local_tgt, *local_tgt_storage = NULL;
    krb5_pa_s4u_x509_user *s4u_x509_user = NULL; /* protocol transition request */
    krb5_authdata **kdc_issued_auth_data = NULL; /* auth data issued by KDC */
    unsigned int c_flags = 0, s_flags = 0;       /* client/server KDB flags */
    krb5_boolean is_referral, is_crossrealm;
    const char *emsg = NULL;
    krb5_kvno ticket_kvno = 0;
    struct kdc_request_state *state = NULL;
    krb5_pa_data *pa_tgs_req; /*points into request*/
    krb5_data scratch;
    krb5_pa_data **e_data = NULL;
    krb5_audit_state *au_state = NULL;
    krb5_data **auth_indicators = NULL;
    krb5_pac header_pac = NULL, stkt_pac = NULL, subject_pac;

    memset(&reply, 0, sizeof(reply));
    memset(&reply_encpart, 0, sizeof(reply_encpart));
    memset(&ticket_reply, 0, sizeof(ticket_reply));
    memset(&enc_tkt_reply, 0, sizeof(enc_tkt_reply));
    memset(&server_keyblock, 0, sizeof(server_keyblock));
    memset(&local_tgt_key, 0, sizeof(local_tgt_key));
    session_key.contents = NULL;

    /* Save pointer to client-requested service principal, in case of
     * errors before a successful call to search_sprinc(). */
    sprinc = request->server;

    if (request->msg_type != KRB5_TGS_REQ) {
        krb5_free_kdc_req(kdc_context, request);
        return KRB5_BADMSGTYPE;
    }

    errcode = kdc_make_rstate(kdc_active_realm, &state);
    if (errcode != 0)
        goto cleanup;

    /* Initialize audit state. */
    errcode = kau_init_kdc_req(kdc_context, request, from, &au_state);
    if (errcode)
        goto cleanup;

    /* Seed the audit trail with the request ID and basic information. */
    kau_tgs_req(kdc_context, TRUE, au_state);

    errcode = kdc_process_tgs_req(kdc_active_realm,
                                  request, from, pkt, &header_ticket,
                                  &header_server, &header_key, &subkey,
                                  &pa_tgs_req);
    if (header_ticket && header_ticket->enc_part2)
        cprinc = header_ticket->enc_part2->client;

    if (errcode) {
        status = "PROCESS_TGS";
        goto cleanup;
    }

    if (!header_ticket) {
        errcode = KRB5_NO_TKT_SUPPLIED;        /* XXX? */
        goto cleanup;
    }
    errcode = kau_make_tkt_id(kdc_context, header_ticket,
                              &au_state->tkt_in_id);
    if (errcode)
        goto cleanup;

    scratch.length = pa_tgs_req->length;
    scratch.data = (char *) pa_tgs_req->contents;
    errcode = kdc_find_fast(&request, &scratch, subkey,
                            header_ticket->enc_part2->session, state, NULL);
    /* Reset sprinc because kdc_find_fast() can replace request. */
    sprinc = request->server;
    if (errcode !=0) {
        status = "FIND_FAST";
        goto cleanup;
    }
    if (sprinc == NULL) {
        status = "NULL_SERVER";
        errcode = KRB5KDC_ERR_S_PRINCIPAL_UNKNOWN;
        goto cleanup;
    }

    errcode = get_local_tgt(kdc_context, &sprinc->realm, header_server,
                            &local_tgt, &local_tgt_storage, &local_tgt_key);
    if (errcode) {
        status = "GET_LOCAL_TGT";
        goto cleanup;
    }

    errcode = get_verified_pac(kdc_context, header_ticket->enc_part2,
                               header_server->princ, header_key, local_tgt,
                               &local_tgt_key, &header_pac);
    if (errcode) {
        status = "HEADER_PAC";
        goto cleanup;
    }

    /* Ignore (for now) the request modification due to FAST processing. */
    au_state->request = request;

    /*
     * Pointer to the encrypted part of the header ticket, which may be
     * replaced to point to the encrypted part of the evidence ticket
     * if constrained delegation is used. This simplifies the number of
     * special cases for constrained delegation.
     */
    header_enc_tkt = header_ticket->enc_part2;

    /*
     * We've already dealt with the AP_REQ authentication, so we can
     * use header_ticket freely.  The encrypted part (if any) has been
     * decrypted with the session key.
     */

    au_state->stage = SRVC_PRINC;

    /* XXX make sure server here has the proper realm...taken from AP_REQ
       header? */

    if (isflagset(request->kdc_options, KDC_OPT_CANONICALIZE))
        setflag(s_flags, KRB5_KDB_FLAG_REFERRAL_OK);

    errcode = search_sprinc(kdc_active_realm, request, s_flags, &server,
                            &status);
    if (errcode != 0)
        goto cleanup;
    sprinc = server->princ;

    /* If we got a cross-realm TGS which is not the requested server, we are
     * issuing a referral (or alternate TGT, which we treat similarly). */
    is_referral = is_cross_tgs_principal(server->princ) &&
        !krb5_principal_compare(kdc_context, request->server, server->princ);

    au_state->stage = VALIDATE_POL;

    if ((errcode = krb5_timeofday(kdc_context, &kdc_time)))
        goto cleanup;

    is_crossrealm = !data_eq(header_server->princ->realm, sprinc->realm);
    if (is_crossrealm)
        setflag(c_flags, KRB5_KDB_FLAG_CROSS_REALM);
    if (is_referral)
        setflag(c_flags, KRB5_KDB_FLAG_ISSUING_REFERRAL);

    /* Check for protocol transition */
    errcode = kdc_process_s4u2self_req(kdc_active_realm, request, server,
                                       subkey, header_enc_tkt->session,
                                       &s4u_x509_user, &client, &status);
    if (s4u_x509_user != NULL || errcode != 0) {
        if (s4u_x509_user != NULL)
            au_state->s4u2self_user = s4u_x509_user->user_id.user;
        au_state->status = status;
        kau_s4u2self(kdc_context, errcode ? FALSE : TRUE, au_state);
        au_state->s4u2self_user = NULL;
        if (errcode)
            goto cleanup;
    }
    if (s4u_x509_user != NULL)
        setflag(c_flags, KRB5_KDB_FLAG_PROTOCOL_TRANSITION);

    /* For user-to-user and S4U2Proxy requests, decrypt the second ticket. */
    errcode = decrypt_2ndtkt(kdc_active_realm, request, c_flags, local_tgt,
                             &local_tgt_key, &stkt, &stkt_pac, &stkt_server,
                             &stkt_server_key, &status);
    if (errcode)
        goto cleanup;

    retval = validate_tgs_request(kdc_active_realm, request, server,
                                  header_ticket, header_pac, stkt, stkt_pac,
                                  stkt_server, kdc_time, s4u_x509_user,
                                  client, is_crossrealm, is_referral,
                                  &status, &e_data);
    if (retval) {
        if (retval == KDC_ERR_POLICY || retval == KDC_ERR_BADOPTION)
            au_state->violation = PROT_CONSTRAINT;
        errcode = retval + ERROR_TABLE_BASE_krb5;
        goto cleanup;
    }

    if (isflagset(request->kdc_options, KDC_OPT_CNAME_IN_ADDL_TKT)) {
        /* Do constrained delegation protocol and authorization checks. */
        setflag(c_flags, KRB5_KDB_FLAG_CONSTRAINED_DELEGATION);

        errcode = kdc_process_s4u2proxy_req(kdc_active_realm, c_flags,
                                            request, header_pac,
                                            stkt->enc_part2, stkt_pac,
                                            stkt_server, stkt_server_key,
                                            header_ticket->enc_part2->client,
                                            server, &stkt_authdata_client,
                                            &status);
        if (errcode == KDC_ERR_POLICY || errcode == KDC_ERR_BADOPTION)
            au_state->violation = PROT_CONSTRAINT;
        else if (errcode)
            au_state->violation = LOCAL_POLICY;
        au_state->status = status;
        retval = kau_make_tkt_id(kdc_context, stkt, &au_state->evid_tkt_id);
        if (retval) {
            errcode = retval;
            goto cleanup;
        }
        kau_s4u2proxy(kdc_context, errcode ? FALSE : TRUE, au_state);
        if (errcode)
            goto cleanup;

        assert(krb5_is_tgs_principal(header_ticket->server));
    }

    au_state->stage = ISSUE_TKT;

    errcode = gen_session_key(kdc_active_realm, request, server, &session_key,
                              &status);
    if (errcode)
        goto cleanup;

    /*
     * subject_tkt will refer to the evidence ticket (for constrained
     * delegation) or the TGT. The distinction from header_enc_tkt is
     * necessary because the TGS signature only protects some fields:
     * the others could be forged by a malicious server.
     */

    if (isflagset(c_flags, KRB5_KDB_FLAG_CONSTRAINED_DELEGATION)) {
        subject_tkt = stkt->enc_part2;
        subject_pac = stkt_pac;
        subject_server = stkt_server;
        subject_key = stkt_server_key;
    } else {
        subject_tkt = header_enc_tkt;
        subject_pac = header_pac;
        subject_server = header_server;
        subject_key = header_key;
    }
    authtime = subject_tkt->times.authtime;

    /* Extract and check auth indicators from the subject ticket, except for
     * S4U2Self requests (where the client didn't authenticate). */
    if (s4u_x509_user == NULL) {
        errcode = get_auth_indicators(kdc_context, subject_tkt, local_tgt,
                                      &local_tgt_key, &auth_indicators);
        if (errcode) {
            status = "GET_AUTH_INDICATORS";
            goto cleanup;
        }

        errcode = check_indicators(kdc_context, server, auth_indicators);
        if (errcode) {
            status = "HIGHER_AUTHENTICATION_REQUIRED";
            goto cleanup;
        }
    }

    if (is_referral)
        ticket_reply.server = server->princ;
    else
        ticket_reply.server = request->server; /* XXX careful for realm... */

    enc_tkt_reply.flags = get_ticket_flags(request->kdc_options, client,
                                           server, header_enc_tkt);
    enc_tkt_reply.times.starttime = 0;

    if (s4u_x509_user != NULL && !is_referral) {
        /* Check if we need to suppress the forwardable ticket flag. */
        errcode = s4u2self_forwardable(kdc_context, server, &enc_tkt_reply);
        if (errcode)
            goto cleanup;
    }

    /* don't use new addresses unless forwarded, see below */

    enc_tkt_reply.caddrs = header_enc_tkt->caddrs;
    /* noaddrarray[0] = 0; */
    reply_encpart.caddrs = 0;/* optional...don't put it in */
    reply_encpart.enc_padata = NULL;

    /*
     * It should be noted that local policy may affect the
     * processing of any of these flags.  For example, some
     * realms may refuse to issue renewable tickets
     */

    if (isflagset(request->kdc_options, KDC_OPT_FORWARDED) ||
        isflagset(request->kdc_options, KDC_OPT_PROXY)) {

        /* include new addresses in ticket & reply */

        enc_tkt_reply.caddrs = request->addresses;
        reply_encpart.caddrs = request->addresses;
    }

    if (isflagset(request->kdc_options, KDC_OPT_POSTDATED))
        enc_tkt_reply.times.starttime = request->from;
    else
        enc_tkt_reply.times.starttime = kdc_time;

    if (isflagset(request->kdc_options, KDC_OPT_VALIDATE)) {
        assert(isflagset(c_flags, KRB5_KDB_FLAGS_S4U) == 0);
        /* BEWARE of allocation hanging off of ticket & enc_part2, it belongs
           to the caller */
        ticket_reply = *(header_ticket);
        enc_tkt_reply = *(header_ticket->enc_part2);
        enc_tkt_reply.authorization_data = NULL;
        clear(enc_tkt_reply.flags, TKT_FLG_INVALID);
    }

    if (isflagset(request->kdc_options, KDC_OPT_RENEW)) {
        krb5_timestamp old_starttime;
        krb5_deltat old_life;

        assert(isflagset(c_flags, KRB5_KDB_FLAGS_S4U) == 0);
        /* BEWARE of allocation hanging off of ticket & enc_part2, it belongs
           to the caller */
        ticket_reply = *(header_ticket);
        enc_tkt_reply = *(header_ticket->enc_part2);
        enc_tkt_reply.authorization_data = NULL;

        old_starttime = enc_tkt_reply.times.starttime ?
            enc_tkt_reply.times.starttime : enc_tkt_reply.times.authtime;
        old_life = ts_delta(enc_tkt_reply.times.endtime, old_starttime);

        enc_tkt_reply.times.starttime = kdc_time;
        enc_tkt_reply.times.endtime =
            ts_min(header_ticket->enc_part2->times.renew_till,
                   ts_incr(kdc_time, old_life));
    } else {
        /* not a renew request */
        enc_tkt_reply.times.starttime = kdc_time;

        kdc_get_ticket_endtime(kdc_active_realm, enc_tkt_reply.times.starttime,
                               header_enc_tkt->times.endtime, request->till,
                               client, server, &enc_tkt_reply.times.endtime);
    }

    kdc_get_ticket_renewtime(kdc_active_realm, request, header_enc_tkt, client,
                             server, &enc_tkt_reply);

    errcode = check_kdcpolicy_tgs(kdc_context, request, server, header_ticket,
                                  auth_indicators, kdc_time,
                                  &enc_tkt_reply.times, &status);
    if (errcode)
        goto cleanup;

    /*
     * Set authtime to be the same as header or evidence ticket's
     */
    enc_tkt_reply.times.authtime = authtime;

    /* starttime is optional, and treated as authtime if not present.
       so we can nuke it if it matches */
    if (enc_tkt_reply.times.starttime == enc_tkt_reply.times.authtime)
        enc_tkt_reply.times.starttime = 0;

    if (isflagset(c_flags, KRB5_KDB_FLAG_PROTOCOL_TRANSITION)) {
        altcprinc = s4u_x509_user->user_id.user;
    } else if (isflagset(c_flags, KRB5_KDB_FLAG_CONSTRAINED_DELEGATION)) {
        /* kdc_process_s4u2proxy_req() only allows cross-realm requests if
         * stkt_authdata_client is set. */
        altcprinc = is_crossrealm ? stkt_authdata_client : subject_tkt->client;
    } else {
        altcprinc = NULL;
    }
    if (isflagset(request->kdc_options, KDC_OPT_ENC_TKT_IN_SKEY)) {
        encrypting_key = stkt->enc_part2->session;
    } else {
        errcode = get_first_current_key(kdc_context, server, &server_keyblock);
        if (errcode) {
            status = "FINDING_SERVER_KEY";
            goto cleanup;
        }
        encrypting_key = &server_keyblock;
    }

    if (isflagset(c_flags, KRB5_KDB_FLAG_PROTOCOL_TRANSITION)) {
        /*
         * For consistency with Active Directory, don't allow authorization
         * data to be disabled if S4U2Self is requested.  The server likely
         * needs a PAC to inspect or for an S4U2Proxy operation, even if it
         * doesn't need authorization data in tickets received from clients.
         */
        clear(server->attributes, KRB5_KDB_NO_AUTH_DATA_REQUIRED);
    }
    if (isflagset(server->attributes, KRB5_KDB_NO_AUTH_DATA_REQUIRED) == 0) {
        /* If we are not doing protocol transition, try to look up the subject
         * principal so that KDB modules can add additional authdata. */
        if (!isflagset(c_flags, KRB5_KDB_FLAG_PROTOCOL_TRANSITION)) {
            setflag(c_flags, KRB5_KDB_FLAG_CLIENT);
            /* Map principals from foreign (possibly non-AD) realms */
            setflag(c_flags, KRB5_KDB_FLAG_MAP_PRINCIPALS);

            assert(client == NULL); /* should not have been set already */

            errcode = krb5_db_get_principal(kdc_context, subject_tkt->client,
                                            c_flags, &client);
        }
    }

    if (isflagset(c_flags, KRB5_KDB_FLAGS_S4U) && !is_referral)
        enc_tkt_reply.client = altcprinc;
    else
        enc_tkt_reply.client = header_enc_tkt->client;

    enc_tkt_reply.session = &session_key;
    enc_tkt_reply.transited.tr_type = KRB5_DOMAIN_X500_COMPRESS;
    enc_tkt_reply.transited.tr_contents = empty_string; /* equivalent of "" */

    /*
     * Only add the realm of the presented tgt to the transited list if
     * it is different than the server realm (cross-realm) and it is different
     * than the realm of the client (since the realm of the client is already
     * implicitly part of the transited list and should not be explicitly
     * listed).
     */
    if (!is_crossrealm ||
        krb5_realm_compare(kdc_context, header_ticket->server,
                           enc_tkt_reply.client)) {
        /* tgt issued by local realm or issued by realm of client */
        enc_tkt_reply.transited = header_enc_tkt->transited;
    } else {
        /* tgt issued by some other realm and not the realm of the client */
        /* assemble new transited field into allocated storage */
        if (header_enc_tkt->transited.tr_type !=
            KRB5_DOMAIN_X500_COMPRESS) {
            status = "VALIDATE_TRANSIT_TYPE";
            errcode = KRB5KDC_ERR_TRTYPE_NOSUPP;
            goto cleanup;
        }
        memset(&enc_tkt_reply.transited, 0, sizeof(enc_tkt_reply.transited));
        enc_tkt_reply.transited.tr_type = KRB5_DOMAIN_X500_COMPRESS;
        if ((errcode =
             add_to_transited(&header_enc_tkt->transited.tr_contents,
                              &enc_tkt_reply.transited.tr_contents,
                              header_ticket->server,
                              enc_tkt_reply.client,
                              request->server))) {
            status = "ADD_TO_TRANSITED_LIST";
            goto cleanup;
        }
        newtransited = 1;
    }
    if (!isflagset (request->kdc_options, KDC_OPT_DISABLE_TRANSITED_CHECK)) {
        errcode = kdc_check_transited_list (kdc_active_realm,
                                            &enc_tkt_reply.transited.tr_contents,
                                            krb5_princ_realm (kdc_context, header_enc_tkt->client),
                                            krb5_princ_realm (kdc_context, request->server));
        if (errcode == 0) {
            setflag (enc_tkt_reply.flags, TKT_FLG_TRANSIT_POLICY_CHECKED);
        } else {
            log_tgs_badtrans(kdc_context, cprinc, sprinc,
                             &enc_tkt_reply.transited.tr_contents, errcode);
        }
    } else
        krb5_klog_syslog(LOG_INFO, _("not checking transit path"));
    if (kdc_active_realm->realm_reject_bad_transit &&
        !isflagset(enc_tkt_reply.flags, TKT_FLG_TRANSIT_POLICY_CHECKED)) {
        errcode = KRB5KDC_ERR_POLICY;
        status = "BAD_TRANSIT";
        au_state->violation = LOCAL_POLICY;
        goto cleanup;
    }

    errcode = handle_authdata(kdc_active_realm, c_flags, client, server,
                              subject_server, local_tgt, &local_tgt_key,
                              subkey != NULL ? subkey :
                              header_ticket->enc_part2->session,
                              encrypting_key, subject_key, NULL, pkt, request,
                              altcprinc, subject_pac, subject_tkt,
                              &auth_indicators, &enc_tkt_reply);
    if (errcode) {
        krb5_klog_syslog(LOG_INFO, _("TGS_REQ : handle_authdata (%d)"),
                         errcode);
        status = "HANDLE_AUTHDATA";
        goto cleanup;
    }

    ticket_reply.enc_part2 = &enc_tkt_reply;

    /* If we are doing user-to-user authentication, encrypt the ticket using
     * the session key of the second ticket. */
    if (isflagset(request->kdc_options, KDC_OPT_ENC_TKT_IN_SKEY)) {
        ticket_kvno = 0;
        ticket_reply.enc_part.enctype = stkt->enc_part2->session->enctype;
        kau_u2u(kdc_context, TRUE, au_state);
    } else {
        ticket_kvno = current_kvno(server);
    }

    errcode = krb5_encrypt_tkt_part(kdc_context, encrypting_key,
                                    &ticket_reply);
    if (errcode)
        goto cleanup;
    ticket_reply.enc_part.kvno = ticket_kvno;
    /* Start assembling the response */
    au_state->stage = ENCR_REP;
    reply.msg_type = KRB5_TGS_REP;
    if (isflagset(c_flags, KRB5_KDB_FLAG_PROTOCOL_TRANSITION) &&
        krb5int_find_pa_data(kdc_context, request->padata,
                             KRB5_PADATA_S4U_X509_USER) != NULL) {
        errcode = kdc_make_s4u2self_rep(kdc_context,
                                        subkey,
                                        header_ticket->enc_part2->session,
                                        s4u_x509_user,
                                        &reply,
                                        &reply_encpart);
        if (errcode)
            au_state->status = status;
        kau_s4u2self(kdc_context, errcode ? FALSE : TRUE, au_state);
        if (errcode)
            goto cleanup;
    }

    reply.client = enc_tkt_reply.client;
    reply.enc_part.kvno = 0;/* We are using the session key */
    reply.ticket = &ticket_reply;

    reply_encpart.session = &session_key;
    reply_encpart.nonce = request->nonce;

    /* copy the time fields */
    reply_encpart.times = enc_tkt_reply.times;

    nolrentry.lr_type = KRB5_LRQ_NONE;
    nolrentry.value = 0;
    nolrentry.magic = 0;
    nolrarray[0] = &nolrentry;
    nolrarray[1] = 0;
    reply_encpart.last_req = nolrarray;        /* not available for TGS reqs */
    reply_encpart.key_exp = 0;/* ditto */
    reply_encpart.flags = enc_tkt_reply.flags;
    reply_encpart.server = ticket_reply.server;

    /* use the session key in the ticket, unless there's a subsession key
       in the AP_REQ */
    reply.enc_part.enctype = subkey ? subkey->enctype :
        header_ticket->enc_part2->session->enctype;
    errcode  = kdc_fast_response_handle_padata(state, request, &reply,
                                               subkey ? subkey->enctype : header_ticket->enc_part2->session->enctype);
    if (errcode)
        goto cleanup;
    errcode =kdc_fast_handle_reply_key(state,
                                       subkey?subkey:header_ticket->enc_part2->session, &reply_key);
    if (errcode)
        goto cleanup;
    errcode = return_enc_padata(kdc_context, pkt, request,
                                reply_key, server, &reply_encpart,
                                is_referral &&
                                isflagset(s_flags, KRB5_KDB_FLAG_REFERRAL_OK));
    if (errcode) {
        status = "KDC_RETURN_ENC_PADATA";
        goto cleanup;
    }

    errcode = kau_make_tkt_id(kdc_context, &ticket_reply, &au_state->tkt_out_id);
    if (errcode)
        goto cleanup;

    if (kdc_fast_hide_client(state))
        reply.client = (krb5_principal)krb5_anonymous_principal();
    errcode = krb5_encode_kdc_rep(kdc_context, KRB5_TGS_REP, &reply_encpart,
                                  subkey ? 1 : 0,
                                  reply_key,
                                  &reply, response);
    if (!errcode)
        status = "ISSUE";

    memset(ticket_reply.enc_part.ciphertext.data, 0,
           ticket_reply.enc_part.ciphertext.length);
    free(ticket_reply.enc_part.ciphertext.data);
    /* these parts are left on as a courtesy from krb5_encode_kdc_rep so we
       can use them in raw form if needed.  But, we don't... */
    memset(reply.enc_part.ciphertext.data, 0,
           reply.enc_part.ciphertext.length);
    free(reply.enc_part.ciphertext.data);

cleanup:
    if (status == NULL)
        status = "UNKNOWN_REASON";
    krb5_free_keyblock_contents(kdc_context, &server_keyblock);
    if (reply_key)
        krb5_free_keyblock(kdc_context, reply_key);
    if (stkt_server_key)
        krb5_free_keyblock(kdc_context, stkt_server_key);
    if (errcode)
        emsg = krb5_get_error_message (kdc_context, errcode);

    if (au_state != NULL) {
        au_state->status = status;
        if (!errcode)
            au_state->reply = &reply;
        kau_tgs_req(kdc_context, errcode ? FALSE : TRUE, au_state);
        kau_free_kdc_req(au_state);
    }

    log_tgs_req(kdc_context, from, request, &reply, cprinc,
                sprinc, altcprinc, authtime,
                c_flags, status, errcode, emsg);
    if (errcode) {
        krb5_free_error_message (kdc_context, emsg);
        emsg = NULL;
    }

    if (errcode && state != NULL) {
        int got_err = 0;
        if (status == 0) {
            status = krb5_get_error_message (kdc_context, errcode);
            got_err = 1;
        }
        errcode -= ERROR_TABLE_BASE_krb5;
        if (errcode < 0 || errcode > KRB_ERR_MAX)
            errcode = KRB_ERR_GENERIC;

        retval = prepare_error_tgs(state, request, header_ticket, errcode,
                                   (server != NULL) ? server->princ : NULL,
                                   response, status, e_data);
        if (got_err) {
            krb5_free_error_message (kdc_context, status);
            status = 0;
        }
    }

    if (header_ticket != NULL)
        krb5_free_ticket(kdc_context, header_ticket);
    if (request != NULL)
        krb5_free_kdc_req(kdc_context, request);
    if (state)
        kdc_free_rstate(state);
    krb5_db_free_principal(kdc_context, server);
    krb5_db_free_principal(kdc_context, stkt_server);
    krb5_db_free_principal(kdc_context, header_server);
    krb5_db_free_principal(kdc_context, client);
    krb5_db_free_principal(kdc_context, local_tgt_storage);
    if (local_tgt_key.contents != NULL)
        krb5_free_keyblock_contents(kdc_context, &local_tgt_key);
    if (session_key.contents != NULL)
        krb5_free_keyblock_contents(kdc_context, &session_key);
    if (newtransited)
        free(enc_tkt_reply.transited.tr_contents.data);
    if (s4u_x509_user != NULL)
        krb5_free_pa_s4u_x509_user(kdc_context, s4u_x509_user);
    if (kdc_issued_auth_data != NULL)
        krb5_free_authdata(kdc_context, kdc_issued_auth_data);
    if (subkey != NULL)
        krb5_free_keyblock(kdc_context, subkey);
    if (header_key != NULL)
        krb5_free_keyblock(kdc_context, header_key);
    if (reply.padata)
        krb5_free_pa_data(kdc_context, reply.padata);
    if (reply_encpart.enc_padata)
        krb5_free_pa_data(kdc_context, reply_encpart.enc_padata);
    if (enc_tkt_reply.authorization_data != NULL)
        krb5_free_authdata(kdc_context, enc_tkt_reply.authorization_data);
    krb5_free_pa_data(kdc_context, e_data);
    k5_free_data_ptr_list(auth_indicators);
    krb5_pac_free(kdc_context, header_pac);
    krb5_pac_free(kdc_context, stkt_pac);
    krb5_free_principal(kdc_context, stkt_authdata_client);

    return retval;
}

static krb5_error_code
prepare_error_tgs (struct kdc_request_state *state,
                   krb5_kdc_req *request, krb5_ticket *ticket, int error,
                   krb5_principal canon_server,
                   krb5_data **response, const char *status,
                   krb5_pa_data **e_data)
{
    krb5_error errpkt;
    krb5_error_code retval = 0;
    krb5_data *scratch, *e_data_asn1 = NULL, *fast_edata = NULL;
    kdc_realm_t *kdc_active_realm = state->realm_data;

    errpkt.magic = KV5M_ERROR;
    errpkt.ctime = 0;
    errpkt.cusec = 0;

    if ((retval = krb5_us_timeofday(kdc_context, &errpkt.stime,
                                    &errpkt.susec)))
        return(retval);
    errpkt.error = error;
    errpkt.server = request->server;
    if (ticket && ticket->enc_part2)
        errpkt.client = ticket->enc_part2->client;
    else
        errpkt.client = NULL;
    errpkt.text.length = strlen(status);
    if (!(errpkt.text.data = strdup(status)))
        return ENOMEM;

    if (!(scratch = (krb5_data *)malloc(sizeof(*scratch)))) {
        free(errpkt.text.data);
        return ENOMEM;
    }

    if (e_data != NULL) {
        retval = encode_krb5_padata_sequence(e_data, &e_data_asn1);
        if (retval) {
            free(scratch);
            free(errpkt.text.data);
            return retval;
        }
        errpkt.e_data = *e_data_asn1;
    } else
        errpkt.e_data = empty_data();

    retval = kdc_fast_handle_error(kdc_context, state, request, e_data,
                                   &errpkt, &fast_edata);
    if (retval) {
        free(scratch);
        free(errpkt.text.data);
        krb5_free_data(kdc_context, e_data_asn1);
        return retval;
    }
    if (fast_edata)
        errpkt.e_data = *fast_edata;
    if (kdc_fast_hide_client(state) && errpkt.client != NULL)
        errpkt.client = (krb5_principal)krb5_anonymous_principal();
    retval = krb5_mk_error(kdc_context, &errpkt, scratch);
    free(errpkt.text.data);
    krb5_free_data(kdc_context, e_data_asn1);
    krb5_free_data(kdc_context, fast_edata);
    if (retval)
        free(scratch);
    else
        *response = scratch;

    return retval;
}

/* KDC options that require a second ticket */
#define STKT_OPTIONS (KDC_OPT_CNAME_IN_ADDL_TKT | KDC_OPT_ENC_TKT_IN_SKEY)
/*
 * If req is a second-ticket request and a second ticket is present, decrypt
 * it.  Set *stkt_out to an alias to the ticket with populated enc_part2.  Set
 * *server_out to the server DB entry and *key_out to the ticket decryption
 * key.
 */
static krb5_error_code
decrypt_2ndtkt(kdc_realm_t *kdc_active_realm, krb5_kdc_req *req,
               krb5_flags flags, krb5_db_entry *local_tgt,
               krb5_keyblock *local_tgt_key, const krb5_ticket **stkt_out,
               krb5_pac *pac_out, krb5_db_entry **server_out,
               krb5_keyblock **key_out, const char **status)
{
    krb5_error_code retval;
    krb5_db_entry *server = NULL;
    krb5_keyblock *key = NULL;
    krb5_kvno kvno;
    krb5_ticket *stkt;

    *stkt_out = NULL;
    *pac_out = NULL;
    *server_out = NULL;
    *key_out = NULL;

    if (!(req->kdc_options & STKT_OPTIONS) || req->second_ticket == NULL ||
        req->second_ticket[0] == NULL)
        return 0;

    stkt = req->second_ticket[0];
    retval = kdc_get_server_key(kdc_context, stkt, flags, TRUE,
                                &server, &key, &kvno);
    if (retval != 0) {
        *status = "2ND_TKT_SERVER";
        goto cleanup;
    }
    retval = krb5_decrypt_tkt_part(kdc_context, key, stkt);
    if (retval != 0) {
        *status = "2ND_TKT_DECRYPT";
        goto cleanup;
    }
    retval = get_verified_pac(kdc_context, stkt->enc_part2, server->princ,
                              key, local_tgt, local_tgt_key, pac_out);
    if (retval != 0) {
        *status = "2ND_TKT_PAC";
        goto cleanup;
    }
    *stkt_out = stkt;
    *server_out = server;
    *key_out = key;
    server = NULL;
    key = NULL;

cleanup:
    krb5_db_free_principal(kdc_context, server);
    krb5_free_keyblock(kdc_context, key);
    return retval;
}

static krb5_error_code
get_2ndtkt_enctype(kdc_realm_t *kdc_active_realm, krb5_kdc_req *req,
                   krb5_enctype *useenctype, const char **status)
{
    krb5_enctype etype;
    krb5_ticket *stkt = req->second_ticket[0];
    int i;

    etype = stkt->enc_part2->session->enctype;
    if (!krb5_c_valid_enctype(etype)) {
        *status = "BAD_ETYPE_IN_2ND_TKT";
        return KRB5KDC_ERR_ETYPE_NOSUPP;
    }
    for (i = 0; i < req->nktypes; i++) {
        if (req->ktype[i] == etype) {
            *useenctype = etype;
            break;
        }
    }
    return 0;
}

static krb5_error_code
gen_session_key(kdc_realm_t *kdc_active_realm, krb5_kdc_req *req,
                krb5_db_entry *server, krb5_keyblock *skey,
                const char **status)
{
    krb5_error_code retval;
    krb5_enctype useenctype = 0;

    /*
     * Some special care needs to be taken in the user-to-user
     * case, since we don't know what keytypes the application server
     * which is doing user-to-user authentication can support.  We
     * know that it at least must be able to support the encryption
     * type of the session key in the TGT, since otherwise it won't be
     * able to decrypt the U2U ticket!  So we use that in preference
     * to anything else.
     */
    if (req->kdc_options & KDC_OPT_ENC_TKT_IN_SKEY) {
        retval = get_2ndtkt_enctype(kdc_active_realm, req, &useenctype,
                                    status);
        if (retval != 0)
            return retval;
    }
    if (useenctype == 0) {
        useenctype = select_session_keytype(kdc_active_realm, server,
                                            req->nktypes,
                                            req->ktype);
    }
    if (useenctype == 0) {
        /* unsupported ktype */
        *status = "BAD_ENCRYPTION_TYPE";
        return KRB5KDC_ERR_ETYPE_NOSUPP;
    }

    return krb5_c_make_random_key(kdc_context, useenctype, skey);
}

/*
 * The request seems to be for a ticket-granting service somewhere else,
 * but we don't have a ticket for the final TGS.  Try to give the requestor
 * some intermediate realm.
 */
static krb5_error_code
find_alternate_tgs(kdc_realm_t *kdc_active_realm, krb5_principal princ,
                   krb5_db_entry **server_ptr, const char **status)
{
    krb5_error_code retval;
    krb5_principal *plist = NULL, *pl2;
    krb5_data tmp;
    krb5_db_entry *server = NULL;

    *server_ptr = NULL;
    assert(is_cross_tgs_principal(princ));
    if ((retval = krb5_walk_realm_tree(kdc_context,
                                       krb5_princ_realm(kdc_context, princ),
                                       krb5_princ_component(kdc_context, princ, 1),
                                       &plist, KRB5_REALM_BRANCH_CHAR))) {
        goto cleanup;
    }
    /* move to the end */
    for (pl2 = plist; *pl2; pl2++);

    /* the first entry in this array is for krbtgt/local@local, so we
       ignore it */
    while (--pl2 > plist) {
        tmp = *krb5_princ_realm(kdc_context, *pl2);
        krb5_princ_set_realm(kdc_context, *pl2,
                             krb5_princ_realm(kdc_context, princ));
        retval = db_get_svc_princ(kdc_context, *pl2, 0, &server, status);
        krb5_princ_set_realm(kdc_context, *pl2, &tmp);
        if (retval == KRB5_KDB_NOENTRY)
            continue;
        else if (retval)
            goto cleanup;

        log_tgs_alt_tgt(kdc_context, server->princ);
        *server_ptr = server;
        server = NULL;
        goto cleanup;
    }
cleanup:
    if (retval == 0 && *server_ptr == NULL)
        retval = KRB5_KDB_NOENTRY;
    if (retval != 0)
        *status = "UNKNOWN_SERVER";

    krb5_free_realm_tree(kdc_context, plist);
    krb5_db_free_principal(kdc_context, server);
    return retval;
}

/* Return true if item is an element of the space/comma-separated list. */
static krb5_boolean
in_list(const char *list, const char *item)
{
    const char *p;
    int len = strlen(item);

    if (list == NULL)
        return FALSE;
    for (p = strstr(list, item); p != NULL; p = strstr(p + 1, item)) {
        if ((p == list || isspace((unsigned char)p[-1]) || p[-1] == ',') &&
            (p[len] == '\0' || isspace((unsigned char)p[len]) ||
             p[len] == ','))
                return TRUE;
    }
    return FALSE;
}

/*
 * Check whether the request satisfies the conditions for generating a referral
 * TGT.  The caller checks whether the hostname component looks like a FQDN.
 */
static krb5_boolean
is_referral_req(kdc_realm_t *kdc_active_realm, krb5_kdc_req *request)
{
    krb5_boolean ret = FALSE;
    char *stype = NULL;
    char *hostbased = kdc_active_realm->realm_hostbased;
    char *no_referral = kdc_active_realm->realm_no_referral;

    if (!(request->kdc_options & KDC_OPT_CANONICALIZE))
        return FALSE;

    if (request->kdc_options & KDC_OPT_ENC_TKT_IN_SKEY)
        return FALSE;

    if (krb5_princ_size(kdc_context, request->server) != 2)
        return FALSE;

    stype = data2string(krb5_princ_component(kdc_context, request->server, 0));
    if (stype == NULL)
        return FALSE;
    switch (krb5_princ_type(kdc_context, request->server)) {
    case KRB5_NT_UNKNOWN:
        /* Allow referrals for NT-UNKNOWN principals, if configured. */
        if (!in_list(hostbased, stype) && !in_list(hostbased, "*"))
            goto cleanup;
        /* FALLTHROUGH */
    case KRB5_NT_SRV_HST:
    case KRB5_NT_SRV_INST:
        /* Deny referrals for specific service types, if configured. */
        if (in_list(no_referral, stype) || in_list(no_referral, "*"))
            goto cleanup;
        ret = TRUE;
        break;
    default:
        goto cleanup;
    }
cleanup:
    free(stype);
    return ret;
}

/*
 * Find a remote realm TGS principal for an unknown host-based service
 * principal.
 */
static krb5_int32
find_referral_tgs(kdc_realm_t *kdc_active_realm, krb5_kdc_req *request,
                  krb5_principal *krbtgt_princ)
{
    krb5_error_code retval = KRB5KDC_ERR_S_PRINCIPAL_UNKNOWN;
    char **realms = NULL, *hostname = NULL;
    krb5_data srealm = request->server->realm;

    if (!is_referral_req(kdc_active_realm, request))
        goto cleanup;

    hostname = data2string(krb5_princ_component(kdc_context,
                                                request->server, 1));
    if (hostname == NULL) {
        retval = ENOMEM;
        goto cleanup;
    }
    /* If the hostname doesn't contain a '.', it's not a FQDN. */
    if (strchr(hostname, '.') == NULL)
        goto cleanup;
    retval = krb5_get_host_realm(kdc_context, hostname, &realms);
    if (retval) {
        /* no match found */
        kdc_err(kdc_context, retval, "unable to find realm of host");
        goto cleanup;
    }
    /* Don't return a referral to the empty realm or the service realm. */
    if (realms == NULL || realms[0] == NULL || *realms[0] == '\0' ||
        data_eq_string(srealm, realms[0])) {
        retval = KRB5KDC_ERR_S_PRINCIPAL_UNKNOWN;
        goto cleanup;
    }
    retval = krb5_build_principal(kdc_context, krbtgt_princ,
                                  srealm.length, srealm.data,
                                  "krbtgt", realms[0], (char *)0);
cleanup:
    krb5_free_host_realm(kdc_context, realms);
    free(hostname);

    return retval;
}

static krb5_error_code
db_get_svc_princ(krb5_context ctx, krb5_principal princ,
                 krb5_flags flags, krb5_db_entry **server,
                 const char **status)
{
    krb5_error_code ret;

    ret = krb5_db_get_principal(ctx, princ, flags, server);
    if (ret == KRB5_KDB_CANTLOCK_DB)
        ret = KRB5KDC_ERR_SVC_UNAVAILABLE;
    if (ret != 0) {
        *status = "LOOKING_UP_SERVER";
    }
    return ret;
}

static krb5_error_code
search_sprinc(kdc_realm_t *kdc_active_realm, krb5_kdc_req *req,
              krb5_flags flags, krb5_db_entry **server, const char **status)
{
    krb5_error_code ret;
    krb5_principal princ = req->server;
    krb5_principal reftgs = NULL;
    krb5_boolean allow_referral;

    /* Do not allow referrals for u2u or ticket modification requests, because
     * the server is supposed to match an already-issued ticket. */
    allow_referral = !(req->kdc_options & NO_REFERRAL_OPTION);
    if (!allow_referral)
        flags &= ~KRB5_KDB_FLAG_REFERRAL_OK;

    ret = db_get_svc_princ(kdc_context, princ, flags, server, status);
    if (ret == 0 || ret != KRB5_KDB_NOENTRY || !allow_referral)
        goto cleanup;

    if (!is_cross_tgs_principal(req->server)) {
        ret = find_referral_tgs(kdc_active_realm, req, &reftgs);
        if (ret != 0)
            goto cleanup;
        ret = db_get_svc_princ(kdc_context, reftgs, flags, server, status);
        if (ret == 0 || ret != KRB5_KDB_NOENTRY)
            goto cleanup;

        princ = reftgs;
    }
    ret = find_alternate_tgs(kdc_active_realm, princ, server, status);

cleanup:
    if (ret != 0 && ret != KRB5KDC_ERR_SVC_UNAVAILABLE) {
        ret = KRB5KDC_ERR_S_PRINCIPAL_UNKNOWN;
        if (*status == NULL)
            *status = "LOOKING_UP_SERVER";
    }
    krb5_free_principal(kdc_context, reftgs);
    return ret;
}
