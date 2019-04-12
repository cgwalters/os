oc new-app --build-env GIT_SSL_NO_VERIFY=1 --name public-redirector \
   registry.access.redhat.com/rhscl/nginx-112-rhel7~https://gitlab.cee.redhat.com/coreos/redhat-coreos.git \
   --context-dir=public-redirector
