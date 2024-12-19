package com.unitedinternet.calendar.ldap;

import com.unboundid.ldap.sdk.*;
import org.springframework.stereotype.Service;

@Service
public class LdapService {

    private final LDAPConnectionPool ldapConnectionPool;

    public LdapService(LDAPConnectionPool ldapConnectionPool) {
        this.ldapConnectionPool = ldapConnectionPool;
    }

    public SearchResult search(String baseDN, String filter) throws LDAPException {
        try (LDAPConnection connection = ldapConnectionPool.getConnection()) {
            return connection.search(baseDN, SearchScope.SUB, filter);
        }
    }

    public void testSearch() {
        try {
            SearchResult result = search("ou=accounts,ou=caldav,ou=services,dc=me,dc=local", "(objectClass=account)");
            System.out.println("Entries found: " + result.getEntryCount());
        } catch (LDAPException e) {
            e.printStackTrace();
        }
    }
}
