NAME=sbc-tracker
VERSION=0.5.1
PKGDIR=$(NAME)-$(VERSION)
SRCDIR=..

.PHONY: deb install clean

deb: install
	dpkg-deb -b $(PKGDIR)
	@echo ""
	@echo "Package built: $(PKGDIR).deb"
	@echo "Install with: sudo dpkg -i $(PKGDIR).deb"

install: clean
	@echo "Building package structure..."
	
	# Create directory structure
	mkdir -p $(PKGDIR)/DEBIAN
	mkdir -p $(PKGDIR)/opt/sbc-tracker/src
	mkdir -p $(PKGDIR)/opt/sbc-tracker/scripts
	mkdir -p $(PKGDIR)/opt/sbc-tracker/systemd
	mkdir -p $(PKGDIR)/etc/sbc-tracker
	mkdir -p $(PKGDIR)/etc/systemd/system
	mkdir -p $(PKGDIR)/var/lib/sbc-tracker/data
	mkdir -p $(PKGDIR)/var/log/sbc-tracker
	mkdir -p $(PKGDIR)/usr/local/bin
	
	# Copy control files
	cp control $(PKGDIR)/DEBIAN/
	cp postinst $(PKGDIR)/DEBIAN/
	cp prerm $(PKGDIR)/DEBIAN/
	chmod 755 $(PKGDIR)/DEBIAN/postinst
	chmod 755 $(PKGDIR)/DEBIAN/prerm
	
	# Copy Python source files
	cp $(SRCDIR)/src/*.py $(PKGDIR)/opt/sbc-tracker/src/
	cp $(SRCDIR)/requirements.txt $(PKGDIR)/opt/sbc-tracker/
	
	# Copy configuration files (as templates)
	cp $(SRCDIR)/config/tracker.conf $(PKGDIR)/etc/sbc-tracker/tracker.conf.example
	
	# Copy scripts
	cp $(SRCDIR)/scripts/*.sh $(PKGDIR)/opt/sbc-tracker/scripts/
	chmod 755 $(PKGDIR)/opt/sbc-tracker/scripts/*.sh
	
	# Copy systemd service
	cp $(SRCDIR)/systemd/ft8-tracker.service $(PKGDIR)/etc/systemd/system/
	
	# Create wrapper script
	echo '#!/bin/bash' > $(PKGDIR)/usr/local/bin/ft8-tracker
	echo 'cd /opt/sbc-tracker' >> $(PKGDIR)/usr/local/bin/ft8-tracker
	echo 'exec /opt/sbc-tracker/venv/bin/python3 /opt/sbc-tracker/src/main.py "$$@"' >> $(PKGDIR)/usr/local/bin/ft8-tracker
	chmod 755 $(PKGDIR)/usr/local/bin/ft8-tracker
	
	@echo "Package structure created"

clean:
	rm -rf $(PKGDIR)
	rm -f $(PKGDIR).deb
	@echo "Cleaned build artifacts"
