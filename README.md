# jacoco-kiuwan-plugin
An utility to import [JaCoCo](https://www.eclemma.org/jacoco/) reports into Kiuwan.
## install and run
Follow these steps to install and run this plugin in your Kiuwan analysis:
1. copy dist/jacoco-kiuwan-plugin-x.y.z.jar in your {KIUWAN_LOCAL_ANALYZER_INSTALLATION_DIR}/lib.custom directory.

2. install dist/ruledef/CUS.MCP.KIUWAN.RULES.JACOCO.Plugin.rule.xml in your Kiuwan account. See more on this at [Installing rule definitions](https://www.kiuwan.com/docs/display/K5/Installing+rule+definitions+created+with+Kiuwan+Rule+Developer)

3. create or edit a model activating this new rule, and assign the model to your kiuwan applications.

4. once the JaCoCo report is inside your source directory, run Kiuwan Local Analyzer program:

    > {kiuwan_local_analyzer_installation_dir}\bin\agent.cmd -n {app_name} -s {src_dir}
    
## how does it work?
This is a technology that allows users to import defects/vulnerabilities into Kiuwan from JaCoCo report files. If the coverage of a file is between 0% and 50% (default values), a violation is generated and reported by Kiuwan. These thresholds can be modified by editing the rule from Kiuwan, as well as the name of the report that the rule will search (jacoco.xml by default).

### rule CUS.MCP.KIUWAN.RULES.JACOCO.Plugin
This Kiuwan plugin is really a Kiuwan native rule that looks for a JaCoCo report file (called jacoco.xml by default) and generates 'Kiuwan defects' for each 'file whose coverage percentage is between the rule thresholds' reported in that file.

You need to upload and insert this rule dist/ruledef/CUS.MCP.KIUWAN.RULES.JACOCO.Plugin.rule.xml in your Kiuwan model to ensure that JaCoCo report is processed.
