/* Test-only Linux uinput gamepad. Never packaged in KolMapper's APK. */
#include <linux/uinput.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
static void checked(int result,const char *what){if(result<0){perror(what);exit(1);}}
int main(void){
 int fd=open("/dev/uinput",O_WRONLY|O_NONBLOCK);checked(fd,"open uinput");
 checked(ioctl(fd,UI_SET_EVBIT,EV_KEY),"enable keys");
 int keys[]={KEY_LEFT,KEY_RIGHT,BTN_A,BTN_X,BTN_TR2};
 for(unsigned i=0;i<sizeof(keys)/sizeof(keys[0]);i++)checked(ioctl(fd,UI_SET_KEYBIT,keys[i]),"enable button");
 checked(ioctl(fd,UI_SET_EVBIT,EV_ABS),"enable axes");checked(ioctl(fd,UI_SET_ABSBIT,ABS_X),"axis x");checked(ioctl(fd,UI_SET_ABSBIT,ABS_Y),"axis y");
 struct uinput_user_dev device;memset(&device,0,sizeof(device));snprintf(device.name,UINPUT_MAX_NAME_SIZE,"KolMapper Test Gamepad");device.id.bustype=BUS_USB;device.id.vendor=1;device.id.product=1;device.id.version=1;
 device.absmin[ABS_X]=device.absmin[ABS_Y]=-32768;device.absmax[ABS_X]=device.absmax[ABS_Y]=32767;
 checked(write(fd,&device,sizeof(device)),"configure device");checked(ioctl(fd,UI_DEV_CREATE),"create gamepad");
 pid_t pid=fork();checked(pid,"fork");if(pid>0){printf("Virtual gamepad daemon pid=%d\n",pid);close(fd);return 0;}
 setsid();FILE *file=fopen("/data/local/tmp/kolmapper-gamepad.pid","w");if(file){fprintf(file,"%d\n",getpid());fclose(file);}
 int null=open("/dev/null",O_RDWR);dup2(null,0);dup2(null,1);dup2(null,2);if(null>2)close(null);
 for(;;)pause();
}
