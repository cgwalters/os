/* This is sourced by the various Jenkins pipeline files. It
 * provides some commonly used functions.
 */

// let's try not to use env vars here to keep things
// decoupled and easier to grok

def define_properties(timer) {

    // Name your test jobs something not starting with rhcos-
    def developmentPipeline = !env.JOB_NAME.startsWith("coreos-rhcos-");

    if (developmentPipeline)
      timer = null;

    echo("Development pipeline: ${developmentPipeline}");

    /* There's a subtle gotcha here. Don't use `env.$PARAM`, but `params.$PARAM`
     * instead. The former will *not* be set on the first run, since the
     * parameters are not set yet. The latter will be set on the first run as
     * soon as the below is executed. See:
     * https://issues.jenkins-ci.org/browse/JENKINS-40574 */
    properties([
      pipelineTriggers(timer == null ? [] : [cron(timer)]),
      parameters([
        credentials(name: 'ARTIFACT_SSH_CREDS_ID',
                    credentialType: 'com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey',
                    description: "SSH key for artifact server.",
                    defaultValue: 'a5990862-8650-411c-9c19-049ee09344e5',
                    required: true),
        credentials(name: 'AWS_CREDENTIALS',
                    credentialType: 'com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl',
                    description: "AWS credentials.",
                    defaultValue: 'd08c733e-63e0-48f4-a2c4-4e060068f94e',
                    required: true),
        credentials(name: 'AWS_CI_ACCOUNT',
                    credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl',
                    description: "OpenShift AWS CI root account number.",
                    defaultValue: '4d186169-c856-4da0-bd9e-0c976c264e83',
                    required: true),
        credentials(name: 'REGISTRY_CREDENTIALS',
                    credentialType: 'com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl',
                    description: "Credentials for Docker registry.",
                    defaultValue: 'e3fd566b-46c1-44e4-aec9-bb59214c1926',
                    required: true),
        credentials(name: 'OPENSHIFT_MIRROR_CREDENTIALS_FILE',
                    credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl',
                    description: "OpenShift shared-secrets mirror cred as file.",
                    defaultValue: '299ad25c-f8d1-4f56-9f00-06a85490321a',
                    required: true),
        credentials(name: 'OPENSHIFT_SSH_CREDS',
                    credentialType: 'com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey',
                    description: "OpenShift github ssh creds",
                    defaultValue: 'ec603426-9cfc-47ad-9075-5aa1c6ffdd7e',
                    required: true),
        // Past here, we're just using parameters as a way to avoid hardcoding internal values; they
        // are not actually secret.
        booleanParam(name: 'FORCE', defaultValue: false, description: 'If true, force a build even if nothing apparently changed'),
        booleanParam(name: 'DRY_RUN', defaultValue: developmentPipeline, description: 'If true, do not push changes'),
        booleanParam(name: 'INIT', defaultValue: false, description: 'If true, assume no previous build history'),
        credentials(name: 'ARTIFACT_SERVER',
                    credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl',
                    description: "(not secret) Server used to push/receive built artifacts.",
                    defaultValue: 'c051c78a-7210-4dec-92de-6f51616aac79',
                    required: true),
        credentials(name: 'S3_PRIVATE_BUCKET',
                    credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl',
                    description: "(not secret) Private S3 bucket to use when uploading AMIs.",
                    defaultValue: '5c9571a2-c492-421f-b506-ba469afffc10',
                    required: true),
        credentials(name: 'S3_PUBLIC_BUCKET',
                     credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl',
                     description: "(not secret) Public S3 bucket to push public files.",
                     defaultValue: '17f001fc-6b07-4233-aee2-3e8448459cf5',
                     required: true),
        credentials(name: 'OSTREE_INSTALL_URL',
                    credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl',
                    description: "(not secret) Remote OSTree repo URL to install from when running imagefactory.",
                    defaultValue: '2d6637ef-7f53-4ee2-bd35-7865908560c7',
                    required: true),
        credentials(name: 'INSTALLER_TREE_URL',
                    credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl',
                    description: "(not secret) Local installer tree mirror to use when running imagefactory.",
                    defaultValue: '50db8fac-f9d8-44e1-af0f-be29325a2896',
                    required: true),
        credentials(name: 'OOTPA_COMPOSE',
                    credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl',
                    description: "(not secret) URL for compose of ootpa base/appstream content",
                    defaultValue: 'fc28db5f-62cc-4386-866d-ea69d2088410',
                    required: true),
        credentials(name: 'OOTPA_BUILDROOT_COMPOSE',
                    credentialType: 'org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl',
                    description: "(not secret) URL for compose of ootpa buildroot content",
                    defaultValue: 'aa396a81-6adc-44e8-8fbc-7ecc54bf883f',
                    required: true),
      ])
    ])
}

def rsync_dir_in(server, key, dir) {
    rsync_dir(key, "${server}:${dir}", dir)
}

def rsync_dir_out(server, key, dir) {
    rsync_dir(key, dir, "${server}:${dir}")
}

def rsync_dir_in_dest(server, key, srcdir, destdir) {
    rsync_dir(key, "${server}:${srcdir}", destdir)
}

def rsync_dir_out_dest(server, key, srcdir, destdir) {
    rsync_dir(key, srcdir, "${server}:${destdir}")
}

def rsync_dir(key, from_dir, to_dir) {
    sh """
        rsync -Hrlpt --stats --delete --delete-after \
            -e 'ssh -i ${key} \
                    -o UserKnownHostsFile=/dev/null \
                    -o StrictHostKeyChecking=no' \
            ${from_dir}/ ${to_dir}
    """
}

def rsync_file_in(server, key, file) {
    rsync_file(key, "${server}:${file}", file)
}

def rsync_file_in_dest(server, key, srcfile, destfile) {
    rsync_file(key, "${server}:${srcfile}", destfile)
}

def rsync_file_out(server, key, file) {
    rsync_file(key, file, "${server}:${file}")
}

def rsync_file_out_dest(server, key, srcfile, destfile) {
    rsync_file(key, srcfile, "${server}:${destfile}")
}

def rsync_file(key, from_file, to_file) {
    sh """
        rsync -Hlpt --stats \
            -e 'ssh -i ${key} \
                    -o UserKnownHostsFile=/dev/null \
                    -o StrictHostKeyChecking=no' \
		    ${from_file} ${to_file}
    """
}

def get_rev_version(repo, rev) {
    version = sh_capture("ostree show --repo=${repo} --print-metadata-key=version ${rev}")
    assert (version.startsWith("'") && version.endsWith("'"))
    return version[1..-2] // trim single quotes
}

def sh_capture(cmd) {
    return sh(returnStdout: true, script: cmd).trim()
}

// Execute a shell script as "builder" user
def sh_builder(cmd) {
    writeFile(file: "${WORKSPACE}/sh-builder", text: "set -xeuo pipefail;\n" + cmd);
    sh """set +x; set -euo pipefail
    chmod a+x ${WORKSPACE}/sh-builder
    runuser -u builder -- env XDG_RUNTIME_DIR=/run/user/1000 ${WORKSPACE}/sh-builder
    rm -f ${WORKSPACE}/sh-builder
    """
}

def registry_login(oscontainer_name, creds) {
    def registry = oscontainer_name.split('/')[0];
    def (username, password) = creds.split(':');
    sh("set +x; podman login -u '${username}' -p '${password}' ${registry}");
}

def registry_login_builder(oscontainer_name, creds) {
    def registry = oscontainer_name.split('/')[0];
    def (username, password) = creds.split(':');
    sh_builder("set +x; podman login -u '${username}' -p '${password}' ${registry}");
}

def openshift_login(url, creds, project) {
    def (username, password) = creds.split(':');
    sh "echo oc login --token '<token>' ${url}";
    sh "set +x; oc login --token '${password}' ${url}";
    sh "oc project '${project}'";
}

// re-implementation of some functionality from scripts/pull-mount-oscontainer
// takes a directory mounted in from the host, creates a new location to
// store containers, and bind mounts it to '/var/lib/containers`
def prep_container_storage(dirFromHost) {
    sh """
        container_storage=/var/lib/containers
        fstype=\$(df -P ${dirFromHost} | awk 'END{print \$6}' | xargs findmnt -n -o FSTYPE)
        if [ \$fstype == 'overlay' ]; then
            echo 'Must supply non-overlay location'
            exit 1
        fi
        rm -rf \${container_storage} && mkdir -p \${container_storage}
        rm -rf ${dirFromHost}/containers && mkdir -p ${dirFromHost}/containers
        mount --bind ${dirFromHost}/containers \${container_storage}
    """
}

// Substitute secrets from credentials into files in the git repo
def prepare_configuration() {
    withCredentials([
      string(credentialsId: params.OOTPA_COMPOSE, variable: 'OOTPA_COMPOSE'),
      string(credentialsId: params.OOTPA_BUILDROOT_COMPOSE, variable: 'OOTPA_BUILDROOT_COMPOSE'),
      file(credentialsId: params.OPENSHIFT_MIRROR_CREDENTIALS_FILE, variable: 'OPENSHIFT_MIRROR_CREDENTIALS_FILE'),
    ]) {
        sh """
        make repo-refresh
        sed -e 's,@OOTPA_COMPOSE@,${OOTPA_COMPOSE},' -e 's,@OOTPA_BUILDROOT_COMPOSE@,${OOTPA_BUILDROOT_COMPOSE},' < ootpa.repo.in > ootpa.repo
        cp ${OPENSHIFT_MIRROR_CREDENTIALS_FILE} ${WORKSPACE}/ops-mirror.pem && sed -i -e "s~WORKSPACE~$WORKSPACE~g" ${WORKSPACE}/cri-o-tested.repo
        """
    }
}

// Helper function to run code inside our assembler container.
// Takes args (string) of extra args for docker, and `fn` to execute.
def inside_coreos_assembler(tag, args, fn) {
    def assembler = "quay.io/cgwalters/coreos-assembler:${tag}"
    docker.image(assembler).pull();
    sh "mkdir remove-entrypoint-build";
    def tmpcontainer;
    dir("remove-entrypoint-build") {
      writeFile(file: "Dockerfile", text: """FROM quay.io/cgwalters/coreos-assembler:${tag}
# https://issues.jenkins-ci.org/browse/JENKINS-33149
ENTRYPOINT []""");
      tmpcontainer = docker.build("localhost/coreos-assembler", ".")
    }
    // Remove the ENTRYPOINT, see https://issues.jenkins-ci.org/browse/JENKINS-33149
    // All of our tasks currently require privileges since they use
    // nested containerization.  We also might as well provide KVM access.
    // Also it'd clearly be better if the inside() API took an array of arguments.
    try {
        tmpcontainer.inside("--privileged --device /dev/kvm ${args}") {
            // We set up XDG_RUNTIME_DIR for both users, since this
            // is where podman reads/writes auth.  Also, chown the workspace.
            sh """
            mkdir -p -m 0755 /run/user
            for x in 0 1000; do 
              mkdir -m 0700 /run/user/\$x
              chown \$x:\$x /run/user/\$x
            done
            chown -R -h 1000:1000 ${WORKSPACE}
            """
            fn()
        }
    } finally {
    //    This method doesn't exist; with Jenkins today we need
    //    to prune images ourselves anyways.
    //    docker.rmi(tmpcontainer.id);
    }
}

def inside_assembler_container(args, fn) {
    // We're using the alpha tag for this legacy API
    def assembler = "quay.io/cgwalters/coreos-assembler:alpha"
    docker.image(assembler).pull();
    docker.image(assembler).inside("--entrypoint \"\" --privileged --device /dev/kvm ${args}") {
      fn()
    }
}

// Send a notification when a job's status changes.
// Treat non-SUCCESS states the same, so it does not send notifications when
// changes go between UNSTABLE and FAILURE.
def notify_status_change(build) {
    def color = ''
    def message = "<${env.BUILD_URL}|Build ${env.BUILD_NUMBER} of ${env.JOB_NAME}>"

    if (build.currentResult == build.previousBuild?.result || params.DRY_RUN)
        return

    if (build.previousBuild?.result == null) {
        echo 'The previous build is still running; ignoring its build state.'
        return
    } else if (build.currentResult == 'SUCCESS') {
        message = ":partyparrot: ${message} is working again."
        color = 'good'
    } else if (build.previousBuild.result == 'SUCCESS') {
        message = ":trashfire: ${message} has started failing."
        color = 'danger'
    } else {
        echo 'This and the previous build have different non-success states.'
        return
    }

    try {
        slackSend channel: '#jenkins-coreos', color: color, message: message
    } catch (NoSuchMethodError err) {
        // Log the message in the console if the Slack plugin is not installed.
        echo message
    }
}

return this
