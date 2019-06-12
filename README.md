# jacoco-kiuwan-plugin
An utility to import [JaCoCo](https://www.eclemma.org/jacoco/) reports into Kiuwan.
## install and run
Follow these steps to install and run this plugin in your Kiuwan analysis:
1. copy dist/jacoco-kiuwan-plugin-x.y.z.jar in your {KIUWAN_LOCAL_ANALYZER_INSTALLATION_DIR}/lib.custom directory.

2. install dist/ruledef/CUS.MCP.KIUWAN.RULES.JACOCO.Plugin.rule.xml in your Kiuwan account. See more on this at [Installing rule definitions](https://www.kiuwan.com/docs/display/K5/Installing+rule+definitions+created+with+Kiuwan+Rule+Developer)

3. create or edit a model activating this new rule, and assign the model to your kiuwan applications.

4. once the JaCoCo report is inside your source directory, run Kiuwan Local Analyzer program:

    > {kiuwan_local_analyzer_installation_dir}\bin\agent.cmd -n {app_name} -s {src_dir}
