// server-private fork: these arrays used to bake in upstream Signal infrastructure IPs at build
// time as a DNS fallback for libsignal-net's pinned routing. The private fork swapped chat
// websockets to OkHttp (which uses Dns.SYSTEM against the user's custom server URL), and the
// remaining libsignal-net callers (CDSI/SVR2/key-transparency) rely on the Environment enum
// baked into the Rust crate, not on these arrays. They are emptied here so a build of this
// fork doesn't ship Signal Inc.'s public IPs.
rootProject.extra["service_ips"] = """new String[]{}"""
rootProject.extra["storage_ips"] = """new String[]{}"""
rootProject.extra["cdn_ips"] = """new String[]{}"""
rootProject.extra["cdn2_ips"] = """new String[]{}"""
rootProject.extra["cdn3_ips"] = """new String[]{}"""
rootProject.extra["sfu_ips"] = """new String[]{}"""
rootProject.extra["content_proxy_ips"] = """new String[]{}"""
rootProject.extra["svr2_ips"] = """new String[]{}"""
rootProject.extra["cdsi_ips"] = """new String[]{}"""
