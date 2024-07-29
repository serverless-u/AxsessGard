#!/bin/sh


# Generate private key
openssl ecparam -name prime256v1 -genkey -noout -out private_key.pem

# Extract public key
openssl ec -in private_key.pem -pubout -out public_key.pem

# Convert private key to PKCS#8 format
openssl pkcs8 -topk8 -nocrypt -in private_key.pem -out private_key_pkcs8.pem

echo "Three pem files were created in the current directory, move them to src/test/resources directory"
