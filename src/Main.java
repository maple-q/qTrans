import org.apache.commons.cli.*;

/**
 * 1、解析用户输入参数，根据不同参数走入不同逻辑
 * 2、正向转发场景 ： A拿到B权限，想访问C，但A直接访问不了C，B可以访问C，A可以访问B，此时在B上做一次转发，将流量从B转发到C即可
 * +---------+           +----------+          +---------+
 * |    A    |    ->     |    B     |   ->     |    C    |
 * +---------+           +----------+          +---------+
 *
 * A  ->   B   ->  C
 *
 * 3、反向转发场景：A拿到B权限，想访问C，A直接访问不了C，A也访问不了B，但是B可以访问A和C，在B上主动去连接 A 和 C，在他们之间转发流量
 *
 *  * +---------+           +----------+          +---------+
 *  * |    A    |    <-     |    B     |   ->     |    C    |
 *  * +---------+           +----------+          +---------+
 *
 *
 * A  <-   B   ->  C
 */

public class Main {
    private final static String MAPPING = "mapping";

    public static void main(String[] args) {
        if (args.length == 0) {
            hintUsage();
            System.exit(-1);
        }

        Options options = new Options();
        // --help
        options.addOption("help", false, "help information");
        // --type
        Option type = Option.builder("type").required(false).hasArg().argName("type").desc("forward type").build();
        options.addOption(type);
        // -l
        Option listenPort = Option.builder("l").required(false).hasArg().argName("listenPort").desc("listening port").build();
        options.addOption(listenPort);
        // -h
        Option remoteHost = Option.builder("h").required(false).hasArg().argName("remoteHost").desc("remote host").build();
        options.addOption(remoteHost);
        // -p
        Option remotePort = Option.builder("p").required(false).hasArg().argName("remotePort").desc("remote port").build();
        options.addOption(remotePort);

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("help")) {
                hintUsage();
                System.exit(-1);
            }

            if (cmd.hasOption("type")) {
                String typeValue = cmd.getOptionValue("type");
                if (MAPPING.equals(typeValue)) {
                    if (!cmd.hasOption("l") || !cmd.hasOption("h") || !cmd.hasOption("p")) {
                        hintUsage();
                        System.exit(-1);
                    }

                    String listenPortUserInput = cmd.getOptionValue("l");
                    String remoteHostUserInput = cmd.getOptionValue("h");
                    String remotePortUserInput = cmd.getOptionValue("p");

                    // 参数校验
                    if (!Utils.isPort(listenPortUserInput) || !Utils.isPort(remotePortUserInput)) {
                        System.out.println("port must be a Integer!");
                        System.exit(-1);
                    }
                    if (!Utils.isIpAddress(remoteHostUserInput)) {
                        System.out.println("Ip address is not valid!!");
                        System.exit(-1);
                    }

                    new Trans(Integer.valueOf(listenPortUserInput), remoteHostUserInput, Integer.valueOf(remotePortUserInput)).start();
                } else {
                    // todo 其他类型
                    hintUsage();
                }
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static void hintUsage() {
        System.out.println("USAGE: ");
        System.out.println("java -jar qTrans.jar -type mapping -l <local_port> -h <remote_host> -p <remote_port>");
        System.out.println("************************************************************************************");
    }
}
