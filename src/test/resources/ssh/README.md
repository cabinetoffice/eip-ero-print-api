
# this command generates a file called `private_key.pem`. 
# The file starts with `-----BEGIN PRIVATE KEY-----`
# The file printer_rsa must begin `-----BEGIN PRIVATE KEY-----`
openssl genrsa -out printer_rsa 2048

# The file `id_rsa.pub` must begin with the text `ssh-rsa`
# To generate such a file run the following command
ssh-keygen -f printer_rsa -y > printer_rsa.pub

[ssh background info](https://docs.moodle.org/dev/SSH_key)
[to read a private key in java](https://stackoverflow.com/a/19387517)