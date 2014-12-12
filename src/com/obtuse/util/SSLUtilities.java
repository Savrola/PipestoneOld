/*
 * Copyright © 2014 Daniel Boulet
 */

package com.obtuse.util;

import com.obtuse.util.exceptions.PipestoneSSLException;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

public class SSLUtilities {

    private static final Map<SSLContextWrapper, SSLContextWrapper> _sslContexts =
            new HashMap<SSLContextWrapper, SSLContextWrapper>();

//    private static class MyTrustManager implements X509TrustManager {
//
//        private final X509TrustManager _realTrustManager;
//
//        private X509Certificate[] _certChain;
//
//        private String _authType;
//
//        private MyTrustManager( X509TrustManager realTrustManager ) {
//            super();
//
//            _realTrustManager = realTrustManager;
//
//        }
//
//        public X509Certificate[] getAcceptedIssuers() {
//
//            throw new UnsupportedOperationException( "we don't support getting the accepted issuers" );
//
//        }
//
//        public void checkClientTrusted( X509Certificate[] certChain, String authType ) {
//
//            throw new UnsupportedOperationException( "we don't support checking if the client is trustworthy" );
//
//        }
//
//        public void checkServerTrusted( X509Certificate[] certChain, String authType )
//                throws CertificateException {
//
//            _certChain = certChain;
//            _authType = authType;
//
//            X509Certificate[] certs = _certChain;
//            if ( certs == null ) {
//
//                Logger.logErr( "no certs captured" );
//
//            } else {
//
//                try {
//
//                    MessageDigest sha1 = MessageDigest.getInstance( "SHA1" );
//                    MessageDigest md5 = MessageDigest.getInstance( "MD5" );
//
//                    Logger.logMsg( "here are the certs:" );
//                    Logger.logMsg( "" );
//
//                    for ( X509Certificate cert : certs ) {
//
//                        Logger.logMsg( "Subject:        " + cert.getSubjectX500Principal() );
//                        Logger.logMsg( "Issuer:         " + cert.getIssuerX500Principal() );
//                        Logger.logMsg(
//                                "effective:      from " + cert.getNotBefore() + " through " + cert.getNotAfter()
//                        );
//                        Logger.logMsg( "serial number:  " + cert.getSerialNumber() );
//                        Logger.logMsg( "sig algorithm:  " + cert.getSigAlgName() );
//                        Logger.logMsg( "version:        " + cert.getVersion() );
//
//                        sha1.update( cert.getEncoded() );
//                        Logger.logMsg( "SHA1:     " + ObtuseUtilx.hexvalue( sha1.digest() ) );
//
//                        md5.update( cert.getEncoded() );
//                        Logger.logMsg( "MD5:      " + ObtuseUtilx.hexvalue( md5.digest() ) );
//
//                        Logger.logMsg( "serialized form is " + ObtuseUtilx.getSerializedSize( cert ) + " bytes long" );
//                        Logger.logMsg( "encoded form is " + cert.getEncoded().length + " bytes long" );
//                        Logger.logMsg( "cert's class is " + cert.getClass() );
//
////                    _myIx += 1;
////                    ks.setCertificateEntry( "balzac-" + _myIx, cert );
//
////                    Logger.logMsg( "added to trusted certs" );
////                    Logger.logMsg( "" );
//
//                    }
//
//                } catch ( NoSuchAlgorithmException e ) {
//
//                    Logger.logErr( "got a NoSuchAlgorithmException looking for SHA1 or MD5 algorithm", e );
//
//                }
//
//            }
//
//            _realTrustManager.checkServerTrusted( certChain, authType );
//
//        }
//
//    }

    /**
     * Carry an {@link javax.net.ssl.SSLContext} around in a package that identifies it by the keystore that was used to create it.
     * This allows us to avoid having dozens of different {@link javax.net.ssl.SSLContext}s which all reference the same keystores.
     */

    private static class SSLContextWrapper {

        private final boolean _clientMode;

        private SSLContext _sslContext;

        private final String _keystoreFname;

        private final char[] _keystorePassword;

        private SSLContextWrapper( boolean clientMode, String keystoreFname, char[] keystorePassword ) {
            super();

            _clientMode = clientMode;
            _keystoreFname = keystoreFname;
            _keystorePassword = keystorePassword.clone();

        }

        private void setSSLContext( SSLContext sslContext ) {

            _sslContext = sslContext;

        }

        private SSLContext getSSLContext() {

            return _sslContext;

        }

        public int hashCode() {

            return _keystoreFname.hashCode() ^ new Integer( _keystorePassword.length ).hashCode();

        }

        @SuppressWarnings( { "EqualsWhichDoesntCheckParameterClass" } )
        public boolean equals( Object xrhs ) {

            try {

                SSLContextWrapper rhs = (SSLContextWrapper)xrhs;
                if ( rhs == null ) {

                    return false;

                }

                if ( _keystoreFname.equals( rhs._keystoreFname ) &&
                     _clientMode == rhs._clientMode &&
                     _keystorePassword.length == rhs._keystorePassword.length ) {

                    for ( int i = 0; i < _keystorePassword.length; i += 1 ) {

                        if ( _keystorePassword[i] != rhs._keystorePassword[i] ) {

                            return false;

                        }

                    }

                    return true;

                }

                return false;

            } catch ( ClassCastException e ) {

                return false;

            }

        }

        public String toString() {

            return "SSLContextWrapper( client mode = " + _clientMode + ", keystore = " + _keystoreFname + " )";

        }

    }

    private SSLUtilities() {
        super();

    }

    /**
     * Get or create an {@link javax.net.ssl.SSLContext} associated with a specified keystore file and password.
     *
     * @param clientMode          true if we want a client-mode context, false otherwise.
     * @param keystoreFileName       the keystore file.
     * @param keystoreInputStream an input stream referring to the keystore file.
     * @param keystorePassword    its password.
     * @param keyPassword         optional key password.
     *
     * @return the SSL context associated with the keystore file and password.
     *
     * @throws com.obtuse.util.exceptions.PipestoneSSLException if the attempt fails.
     */

    public static SSLContext getSSLContext(
            boolean clientMode,
            String keystoreFileName,
            InputStream keystoreInputStream,
            char[] keystorePassword,
            @Nullable char[] keyPassword
    )
            throws
            PipestoneSSLException {

        synchronized ( SSLUtilities._sslContexts ) {

            SSLContextWrapper tmp = new SSLContextWrapper( clientMode, keystoreFileName, keystorePassword );
            if ( SSLUtilities._sslContexts.containsKey( tmp ) ) {

//                Logger.logMsg( "reusing SSL cert(s) from " + keystoreFileName );
                return SSLUtilities._sslContexts.get( tmp ).getSSLContext();

            } else {

//                Logger.logMsg( "loading SSL cert(s) from " + keystoreFileName );

                tmp.setSSLContext(
                        SSLUtilities.createWrappedSSLContext(
                                clientMode,
                                keystoreInputStream,
                                keystorePassword,
                                keyPassword
                        )
                );

                SSLUtilities._sslContexts.put( tmp, tmp );
                return tmp.getSSLContext();

            }

        }

    }

//    /**
//     * Get or create an {@link javax.net.ssl.SSLContext} associated with a specified keystore file and password.
//     *
//     * @param clientMode          true if we want a client-mode context, false otherwise.
//     * @param keystoreFile       the keystore file.
//     * @param keystorePassword    its password.
//     * @param keyPassword         optional key password.
//     * @return the SSL context associated with the keystore file and password.
//     * @throws com.obtuse.garnett.exceptions.GarnettSSLChannelCreationFailedException
//     *          if the attempt fails.
//     */
//
//    public static SSLContext getSSLContext(
//            boolean clientMode,
//            File keystoreFile,
//            char[] keystorePassword,
//            char[] keyPassword
//    )
//            throws
//            GarnettSSLChannelCreationFailedException, FileNotFoundException {
//
//        FileInputStream keystoreInputStream = null;
//        try {
//
//            keystoreInputStream = new FileInputStream( keystoreFile );
//            return getSSLContext( clientMode, keystoreFile, keystoreInputStream, keystorePassword, keyPassword );
//
//        } finally {
//
//            ObtuseUtil.closeQuietly( keystoreInputStream );
//
//        }
//
//    }

    public static SSLContext getOurClientSSLContext()
            throws PipestoneSSLException, IOException {

        return SSLUtilities.getSSLContext(
                true,
                "GarnettClient.keystore",
                ResourceUtils.openResource( "GarnettClient.keystore", "net/kenosee/garnett/resources" ),
                // new char[] { 'p', 'i', 'c', 'k', 'l', 'e', 's' }
                "LondonStrumps".toCharArray(),
                null
        );

    }

    private static SSLContext createSSLContext(
            boolean clientMode,
            InputStream keyStoreInputStream,
            char[] keystorePassword,
            char[] keyPassword
    )
            throws
            KeyStoreException,
            IOException,
            NoSuchAlgorithmException,
            CertificateException,
            KeyManagementException,
            UnrecoverableKeyException {

        KeyStore keyStore = KeyStore.getInstance( "JKS" );
        keyStore.load( keyStoreInputStream, keystorePassword );

        SSLContext sslContext = SSLContext.getInstance( "TLS" );

        if ( clientMode ) {

//            TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
//            tmf.init( keyStore );
//            TrustManager[] tms = tmf.getTrustManagers();
//            for ( TrustManager tm : tms ) {
//                Logger.logMsg( "got trust manager " + tm );
//            }
//
//            MyTrustManager myTrustManager = new MyTrustManager( (X509TrustManager)tms[0] );
//
//            sslContext.init( null, new TrustManager[] { myTrustManager }, null );

            TrustManagerFactory tmf = TrustManagerFactory.getInstance( "SunX509" );
            tmf.init( keyStore );
            sslContext.init( null, tmf.getTrustManagers(), null );

        } else {

            KeyManagerFactory kmf = KeyManagerFactory.getInstance( "SunX509" );
            kmf.init( keyStore, keyPassword );
            sslContext.init( kmf.getKeyManagers(), null, null );

        }

        return sslContext;

    }

//    public static SSLContext findOrCreateSslContext(
//            boolean clientMode,
//            File keystoreFile,
//            char[] keystorePassword,
//            char[] keyPassword
//    )
//            throws GarnettInvalidCharacterException, GarnettSSLChannelCreationFailedException, FileNotFoundException {
//
//        synchronized ( _knownSslContexts ) {
//
//            byte[] obfuscatedKeystorePassword = UserUtilities.obfuscate( keystorePassword );
//            byte[] obfuscatedKeyPassword = UserUtilities.obfuscate( keyPassword );
//            String key =
//                    "" + clientMode + ";" + keystoreFile + ";" + ObtuseUtil.hexvalue( obfuscatedKeystorePassword ) +
//                    ";" + ObtuseUtil.hexvalue( obfuscatedKeyPassword );
//
//            SSLContext sslContext = _knownSslContexts.get( key );
//            if ( sslContext == null ) {
//
//                sslContext = SSLUtilities.createWrappedSSLContext(
//                        clientMode,
//                        keystoreFile,
//                        keystorePassword,
//                        keyPassword
//                );
//
//            }
//
//            return sslContext;
//
//        }
//
//    }

    @SuppressWarnings("UnusedDeclaration")
    public static SSLContext createWrappedSSLContext(
            boolean clientMode,
            File keystoreFile,
            char[] keystorePassword,
            char[] keyPassword
    )
            throws FileNotFoundException, PipestoneSSLException {

        FileInputStream keyStoreInputStream = null;

        try {

            keyStoreInputStream = new FileInputStream( keystoreFile );
            return SSLUtilities.createWrappedSSLContext( clientMode, keyStoreInputStream, keystorePassword, keyPassword );

        } finally {

            ObtuseUtil.closeQuietly( keyStoreInputStream );

        }

    }

    public static SSLContext createWrappedSSLContext(
            boolean clientMode, InputStream keyStoreInputStream, char[] keystorePassword, char[] keyPassword
    )
            throws
            PipestoneSSLException {

        try {

            return SSLUtilities.createSSLContext( clientMode, keyStoreInputStream, keystorePassword, keyPassword );

        } catch ( NoSuchAlgorithmException e ) {

            throw new PipestoneSSLException( "caught a NoSuchAlgorithmException", e );

        } catch ( KeyManagementException e ) {

            throw new PipestoneSSLException( "caught a KeyManagementException", e );

        } catch ( FileNotFoundException e ) {

            throw new PipestoneSSLException( "caught a FileNotFoundException", e );

        } catch ( IOException e ) {

            throw new PipestoneSSLException( "caught an IOException", e );

        } catch ( CertificateException e ) {

            throw new PipestoneSSLException( "caught a CertificateException", e );

        } catch ( KeyStoreException e ) {

            throw new PipestoneSSLException( "caught a KeyStoreException", e );

        } catch ( UnrecoverableKeyException e ) {

            throw new PipestoneSSLException( "caught an UnrecoverableKeyException", e );

        }

    }

}