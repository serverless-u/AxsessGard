#!/bin/sh

#
# Generate private and public key for testing
# The generated key are stored in the test resources directory

mkdir -p src/test/resources

# Generate private key
openssl ecparam -name prime256v1 -genkey -noout -out src/test/resources/private_key.pem

# Extract public key
openssl ec -in src/test/resources/private_key.pem -pubout -out src/test/resources/public_key.pem

# Convert private key to PKCS#8 format
openssl pkcs8 -topk8 -nocrypt -in src/test/resources/private_key.pem -out src/test/resources/private_key_pkcs8.pem